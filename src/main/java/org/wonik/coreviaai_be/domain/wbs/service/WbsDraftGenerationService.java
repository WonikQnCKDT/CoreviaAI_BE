package org.wonik.coreviaai_be.domain.wbs.service;

import java.time.Clock;
import java.util.UUID;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
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
 * AI WBS 초안 생성(FR-PRJ-07). 초안은 저장하지 않고 응답으로만 돌려준다(NFR-08: 승인 전 저장 없음).
 * <p>
 * 흐름: 입력 조회(읽기 트랜잭션) → AI 호출(트랜잭션 밖) → 반영 토큰 발급 → 감사 로그(AI 요청, NFR-07).
 * AI 호출이 실패해도 요청 사실과 실패 코드를 감사 로그에 남긴다. 요청 원문은 길고 민감할 수 있어 길이만 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(WbsAiDraftProperties.class)
public class WbsDraftGenerationService {

	private static final String OUTCOME_SUCCESS = "SUCCESS";

	private final WbsDraftContextReader contextReader;
	private final WbsDraftAiClient aiClient;
	private final WbsDraftTokenStore tokenStore;
	private final WbsAiDraftProperties properties;
	private final AuditLogger auditLogger;
	private final TransactionTemplate transactionTemplate;
	private final Clock clock;

	public GeneratedWbsDraft generate(final AuditActor actor, final Long projectId, final GenerateWbsDraftCommand command) {
		final WbsDraftContext context = contextReader.read(actor.userId(), projectId, command);
		final WbsDraftProposal proposal;
		try {
			proposal = aiClient.generate(context);
		} catch (final CustomException e) {
			recordFailedRequest(actor, projectId, command, e);
			throw e;
		}
		final String draftToken = UUID.randomUUID().toString();
		tokenStore.issue(draftToken, new DraftTokenOwner(projectId, actor.userId()));
		recordRequest(actor, projectId, command, OUTCOME_SUCCESS, proposal);
		return new GeneratedWbsDraft(draftToken, clock.instant().plus(properties.tokenTtl()), proposal);
	}

	/** 실패 기록이 안 되더라도 사용자에게는 원래 AI 오류(503·504 등)를 돌려준다. */
	private void recordFailedRequest(
		final AuditActor actor,
		final Long projectId,
		final GenerateWbsDraftCommand command,
		final CustomException failure
	) {
		try {
			recordRequest(actor, projectId, command, failure.getErrorCode().name(), null);
		} catch (final RuntimeException auditFailure) {
			log.error("AI 초안 생성 실패를 감사 로그에 남기지 못했습니다: projectId={}", projectId, auditFailure);
		}
	}

	private void recordRequest(
		final AuditActor actor,
		final Long projectId,
		final GenerateWbsDraftCommand command,
		final String outcome,
		final WbsDraftProposal proposal
	) {
		final AuditChanges changes = AuditChanges.create()
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
