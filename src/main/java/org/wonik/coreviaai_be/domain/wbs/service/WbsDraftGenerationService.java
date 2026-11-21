package org.wonik.coreviaai_be.domain.wbs.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.wonik.coreviaai_be.domain.aidraft.entity.type.AiDraftFeature;
import org.wonik.coreviaai_be.domain.aidraft.service.AiDraftRecorder;
import org.wonik.coreviaai_be.domain.wbs.client.WbsDraftAiClient;
import org.wonik.coreviaai_be.domain.wbs.drafttoken.DraftTokenOwner;
import org.wonik.coreviaai_be.domain.wbs.drafttoken.WbsAiDraftProperties;
import org.wonik.coreviaai_be.domain.wbs.drafttoken.WbsDraftTokenStore;
import org.wonik.coreviaai_be.domain.wbs.service.command.GenerateWbsDraftCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.WbsDraftContext;
import org.wonik.coreviaai_be.domain.wbs.service.result.GeneratedWbsDraft;
import org.wonik.coreviaai_be.domain.wbs.service.result.WbsDraftProposal;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditChanges;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI WBS 초안 생성(FR-PRJ-07). AI 결과는 ai_drafts에 DRAFT로 기록하고, WBS 테이블은 PM이 승인해 반영할 때만 바꾼다
 * (NFR-08 사람 승인).
 * <p>
 * 흐름: 입력 조회(읽기 트랜잭션) → AI 호출(트랜잭션 밖) → ai_drafts 기록(자체 트랜잭션) → 반영 토큰 발급 →
 * 감사 로그(AI 요청, NFR-07). AI 호출이 실패하면 ai_drafts에 FAILED로, 감사 로그에 실패 코드를 남긴다.
 * 요청 원문은 길고 민감할 수 있어 어디에도 저장하지 않고 감사 로그에 길이만 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(WbsAiDraftProperties.class)
public class WbsDraftGenerationService {

	private static final String OUTCOME_SUCCESS = "SUCCESS";

	private final WbsDraftContextReader contextReader;
	private final WbsDraftAiClient aiClient;
	private final AiDraftRecorder aiDraftRecorder;
	private final WbsDraftTokenStore tokenStore;
	private final WbsAiDraftProperties properties;
	private final AuditLogger auditLogger;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	public GeneratedWbsDraft generate(final AuditActor actor, final Long projectId, final GenerateWbsDraftCommand command) {
		final WbsDraftContext context = contextReader.read(actor.userId(), projectId, command);
		final Instant startedAt = clock.instant();
		final WbsDraftProposal proposal;
		try {
			proposal = aiClient.generate(context);
		} catch (final CustomException e) {
			recordFailedRequest(actor, projectId, command, e, Duration.between(startedAt, clock.instant()));
			throw e;
		}
		final Long aiDraftId = aiDraftRecorder.recordDraft(AiDraftFeature.WBS_DRAFT, projectId, actor.userId(), proposal,
			Duration.between(startedAt, clock.instant()));
		final String draftToken = UUID.randomUUID().toString();
		tokenStore.issue(draftToken, new DraftTokenOwner(projectId, actor.userId(), aiDraftId));
		recordRequest(actor, projectId, command, OUTCOME_SUCCESS, proposal, aiDraftId);
		return new GeneratedWbsDraft(draftToken, clock.instant().plus(properties.tokenTtl()), proposal);
	}

	/**
	 * ai_drafts(FAILED)와 감사 로그를 각각 남긴다. 한쪽 기록이 실패해도 다른 쪽은 남기고,
	 * 사용자에게는 원래 AI 오류(503·504 등)를 돌려준다.
	 */
	private void recordFailedRequest(
		final AuditActor actor,
		final Long projectId,
		final GenerateWbsDraftCommand command,
		final CustomException failure,
		final Duration latency
	) {
		Long aiDraftId = null;
		try {
			aiDraftId = aiDraftRecorder.recordFailure(AiDraftFeature.WBS_DRAFT, projectId, actor.userId(), latency);
		} catch (final RuntimeException recordFailure) {
			log.error("AI 초안 생성 실패를 ai_drafts에 남기지 못했습니다: projectId={}", projectId, recordFailure);
		}
		try {
			recordRequest(actor, projectId, command, failure.getErrorCode().name(), null, aiDraftId);
		} catch (final RuntimeException auditFailure) {
			log.error("AI 초안 생성 실패를 감사 로그에 남기지 못했습니다: projectId={}", projectId, auditFailure);
		}
	}

	private void recordRequest(
		final AuditActor actor,
		final Long projectId,
		final GenerateWbsDraftCommand command,
		final String outcome,
		final WbsDraftProposal proposal,
		final Long aiDraftId
	) {
		final AuditChanges changes = AuditChanges.create()
			.value("aiDraftId", aiDraftId)
			.value("purposeLength", lengthOf(command.projectPurpose()))
			.value("requirementsLength", lengthOf(command.requirementsText()))
			.value("outcome", outcome);
		if (proposal != null) {
			changes.value("proposedTaskCount", proposal.taskCount())
				.value("proposedSubtaskCount", proposal.subtaskCount())
				.value("proposedMilestoneCount", proposal.milestones().size())
				.value("feasibility", proposal.feasibility() == null ? null : proposal.feasibility().status());
		}
		transactionTemplate.executeWithoutResult(status -> auditLogger.record(
			actor, AuditAction.AI_WBS_DRAFT_REQUESTED, AuditTargetType.PROJECT, projectId, changes));
	}

	private static int lengthOf(final String value) {
		return value == null ? 0 : value.length();
	}
}
