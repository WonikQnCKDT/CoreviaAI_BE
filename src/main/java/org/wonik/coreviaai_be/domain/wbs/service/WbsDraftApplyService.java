package org.wonik.coreviaai_be.domain.wbs.service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.wonik.coreviaai_be.domain.aidraft.entity.type.AiDraftFeature;
import org.wonik.coreviaai_be.domain.aidraft.entity.type.AiDraftStatus;
import org.wonik.coreviaai_be.domain.aidraft.service.AiDraftRecorder;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberRepository;
import org.wonik.coreviaai_be.domain.wbs.drafttoken.DraftTokenOwner;
import org.wonik.coreviaai_be.domain.wbs.drafttoken.WbsDraftTokenStore;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsDraftAccessPolicy;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsDraftValidator;
import org.wonik.coreviaai_be.domain.wbs.service.command.ApplyWbsDraftCommand;
import org.wonik.coreviaai_be.domain.wbs.service.result.AppliedWbsDraft;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditChanges;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PM이 승인한 AI 초안 항목을 WBS에 반영한다(FR-PRJ-07: PM 승인 항목만 WBS 등록, FR-AIR-09 승인 원칙).
 * <p>
 * 멱등성: 초안 토큰은 한 번만 쓸 수 있다. 재전송은 409 AI_DRAFT_ALREADY_APPLIED, 만료·다른 프로젝트 토큰은
 * 404 AI_DRAFT_EXPIRED로 끝나며 WBS는 한 번만 만들어진다.
 * <ol>
 *   <li>PM 확인 후 토큰 상태 확인(권한 없는 사용자에게 토큰 상태를 알려 주지 않음)</li>
 *   <li>토큰을 원자적으로 소비해 동시 반영을 막는다</li>
 *   <li>한 트랜잭션에서 검증 → 등록 → ai_drafts 결정 기록(ACCEPTED, 제외 항목이 있으면 PARTIALLY_ACCEPTED) → 감사 로그</li>
 *   <li>트랜잭션이 실패하면(검증 오류 포함) 토큰을 되돌려 PM이 고친 뒤 다시 반영할 수 있게 한다</li>
 * </ol>
 * 토큰 복구 자체가 실패하면(Redis 장애) 초안을 다시 생성해야 한다. 이 경우 WBS는 저장되지 않은 상태라 데이터는 일관된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WbsDraftApplyService {

	private final WbsDraftAccessPolicy draftAccessPolicy;
	private final ProjectMemberRepository memberRepository;
	private final WbsDraftValidator validator;
	private final WbsItemWriter itemWriter;
	private final AiDraftRecorder aiDraftRecorder;
	private final WbsDraftTokenStore tokenStore;
	private final AuditLogger auditLogger;
	private final TransactionTemplate transactionTemplate;

	public AppliedWbsDraft apply(final AuditActor actor, final Long projectId, final ApplyWbsDraftCommand command) {
		draftAccessPolicy.getDraftableProject(actor.userId(), projectId);
		final String token = command.draftToken();
		final DraftTokenOwner owner = requireIssuedTo(token, projectId, actor.userId());
		if (!tokenStore.consume(token)) {
			throw new CustomException(ErrorCode.AI_DRAFT_ALREADY_APPLIED);
		}

		final AppliedWbsDraft applied;
		try {
			applied = transactionTemplate.execute(
				status -> applyInTransaction(actor, projectId, owner.aiDraftId(), command));
		} catch (final RuntimeException e) {
			restoreToken(token, owner);
			throw e;
		}
		markApplied(token, owner);
		return applied;
	}

	/** WBS는 이미 저장됐으므로 표시 실패로 응답을 실패시키지 않는다. 표시가 없으면 재전송은 409 대신 404로 끝난다. */
	private void markApplied(final String token, final DraftTokenOwner owner) {
		try {
			tokenStore.markApplied(token, owner);
		} catch (final RuntimeException e) {
			log.warn("AI 초안 반영 완료 표시를 남기지 못했습니다: projectId={}", owner.projectId(), e);
		}
	}

	private DraftTokenOwner requireIssuedTo(final String token, final Long projectId, final Long userId) {
		final Optional<DraftTokenOwner> issued = tokenStore.findIssued(token);
		if (issued.isPresent() && issued.get().isOwnedBy(projectId, userId)) {
			return issued.get();
		}
		final boolean appliedByOwner = issued.isEmpty()
			&& tokenStore.findApplied(token).filter(applied -> applied.isOwnedBy(projectId, userId)).isPresent();
		if (appliedByOwner) {
			throw new CustomException(ErrorCode.AI_DRAFT_ALREADY_APPLIED);
		}
		throw new CustomException(ErrorCode.AI_DRAFT_EXPIRED);
	}

	private AppliedWbsDraft applyInTransaction(
		final AuditActor actor,
		final Long projectId,
		final Long aiDraftId,
		final ApplyWbsDraftCommand command
	) {
		// 토큰 확인 이후 PM 해제·기능 해제가 끼어들 수 있어 트랜잭션 안에서 다시 확인한다.
		final Project project = draftAccessPolicy.getDraftableProject(actor.userId(), projectId);
		final Set<Long> memberUserIds = memberRepository.findAllByProjectIdOrderById(projectId).stream()
			.map(ProjectMember::getUserId)
			.collect(Collectors.toSet());
		validator.validate(command, project.getStartDate(), project.getEndDate(), memberUserIds);

		final AppliedWbsDraft applied = itemWriter.writeAiDraft(projectId, actor.userId(), command);
		aiDraftRecorder.decide(aiDraftId, AiDraftFeature.WBS_DRAFT, projectId, actor.userId(),
			command.rejectedCount() > 0 ? AiDraftStatus.PARTIALLY_ACCEPTED : AiDraftStatus.ACCEPTED);
		auditLogger.record(actor, AuditAction.AI_WBS_DRAFT_APPLIED, AuditTargetType.PROJECT, projectId,
			AuditChanges.create()
				.value("aiDraftId", aiDraftId)
				.value("taskCount", command.tasks().size())
				.value("subtaskCount", applied.taskIds().size() - command.tasks().size())
				.value("milestoneCount", applied.milestoneIds().size())
				.value("rejectedCount", command.rejectedCount()));
		return applied;
	}

	private void restoreToken(final String token, final DraftTokenOwner owner) {
		try {
			tokenStore.issue(token, owner);
		} catch (final RuntimeException restoreFailure) {
			log.error("AI 초안 토큰을 되돌리지 못했습니다. PM은 초안을 다시 생성해야 합니다: projectId={}", owner.projectId(),
				restoreFailure);
		}
	}
}
