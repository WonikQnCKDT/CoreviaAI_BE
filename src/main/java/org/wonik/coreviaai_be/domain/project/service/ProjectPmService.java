package org.wonik.coreviaai_be.domain.project.service;

import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.type.MemberChangeType;
import org.wonik.coreviaai_be.domain.project.entity.type.PreviousPmAction;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.policy.ProjectAccessPolicy;
import org.wonik.coreviaai_be.domain.project.policy.ProjectMemberPolicy;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberHistoryRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectPhaseRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectRepository;
import org.wonik.coreviaai_be.domain.project.service.command.DelegatePmCommand;
import org.wonik.coreviaai_be.domain.project.service.command.ReassignPmCommand;
import org.wonik.coreviaai_be.domain.project.service.result.ProjectPmResult;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditChanges;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * PM 교체(FR-PRJ-04). 프로젝트당 PM은 1명이고, 아래 두 경로로만 바뀐다.
 * <ul>
 *   <li>위임: 현 PM만 다른 활성 멤버에게 넘긴다. 기존 PM은 다른 지정 액터로 바뀌거나 멤버에서 빠진다.</li>
 *   <li>재지정: PM이 비활성(퇴사 등)이라 위임할 수 없을 때 시스템 관리자가 인사 발령으로 정해진 새 PM을 배치한다
 *       (요구사항 명세서 3.1 예외). 새 PM이 멤버가 아니면 PM으로 추가하고, 기존 PM은 멤버에서 뺀다.</li>
 * </ul>
 * 두 경로 모두 게이트 승인자는 PM이 될 수 없다(승인자 지정을 먼저 해제). 프로젝트 행의 version이 올라가므로
 * 같은 프로젝트에서 위임·재지정이 겹치면 나중 요청은 409로 끝난다.
 */
@Service
@RequiredArgsConstructor
public class ProjectPmService {

	private final ProjectAccessPolicy accessPolicy;
	private final ProjectMemberPolicy memberPolicy;
	private final ProjectMemberService memberService;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository memberRepository;
	private final ProjectMemberHistoryRepository historyRepository;
	private final ProjectPhaseRepository phaseRepository;
	private final UserRepository userRepository;
	private final AuditLogger auditLogger;

	@Transactional
	public ProjectPmResult delegate(final AuditActor actor, final Long projectId, final DelegatePmCommand command) {
		final Project project = accessPolicy.getManagedProject(actor.userId(), projectId);
		final Long previousPmUserId = project.getPmUserId();
		final ProjectMember target = memberRepository.findByProjectIdAndUserId(projectId, command.newPmUserId())
			.orElse(null);
		final boolean targetActive = userRepository.findById(command.newPmUserId()).map(User::isActive).orElse(false);
		final boolean targetIsApprover = phaseRepository.existsByProjectIdAndGateApproverUserId(projectId, command.newPmUserId());
		final Set<ProjectActor> assignedActors = memberService.assignedActorsOf(projectId);
		memberPolicy.validateDelegation(assignedActors, previousPmUserId, command, target, targetActive, targetIsApprover);

		final ProjectActor targetBefore = target.getActor();
		target.changeActor(ProjectActor.PM);
		final ProjectMember previousPm = getPmMember(projectId, previousPmUserId);
		final ProjectActor previousPmAfter;
		if (command.previousPmAction() == PreviousPmAction.CHANGE_ROLE) {
			previousPm.changeActor(command.previousPmActor());
			previousPmAfter = command.previousPmActor();
		} else {
			memberRepository.delete(previousPm);
			previousPmAfter = null;
		}
		project.changePm(command.newPmUserId());

		historyRepository.save(memberService.history(
			project, command.newPmUserId(), MemberChangeType.PM_DELEGATED, targetBefore, ProjectActor.PM, actor));
		historyRepository.save(memberService.history(
			project, previousPmUserId, MemberChangeType.PM_DELEGATED, ProjectActor.PM, previousPmAfter, actor));
		auditLogger.record(actor, AuditAction.PROJECT_PM_DELEGATED, AuditTargetType.PROJECT, projectId, AuditChanges.create()
			.change("pmUserId", previousPmUserId, command.newPmUserId())
			.value("previousPmAction", command.previousPmAction())
			.value("previousPmActor", previousPmAfter));
		return resultOf(project);
	}

	/**
	 * 없는 코드와 PM이 활성인 프로젝트를 같은 오류(PM_REASSIGN_NOT_ALLOWED)로 알린다.
	 * 관리자는 프로젝트 데이터를 볼 수 없으므로(3.1), 코드를 바꿔 가며 프로젝트 존재 여부를 알아낼 수 없게 하기 위해서다.
	 */
	@Transactional
	public ProjectPmResult reassign(final AuditActor actor, final ReassignPmCommand command) {
		final Project project = projectRepository.findByCode(command.projectCode())
			.orElseThrow(() -> new CustomException(ErrorCode.PM_REASSIGN_NOT_ALLOWED));
		final Long previousPmUserId = project.getPmUserId();
		final boolean currentPmActive = userRepository.findById(previousPmUserId).map(User::isActive).orElse(false);
		if (currentPmActive) {
			throw new CustomException(ErrorCode.PM_REASSIGN_NOT_ALLOWED);
		}
		memberService.requireInvitableUsers(Set.of(command.newPmUserId()));
		if (phaseRepository.existsByProjectIdAndGateApproverUserId(project.getId(), command.newPmUserId())) {
			throw new CustomException(ErrorCode.PM_TARGET_IS_GATE_APPROVER);
		}

		final Optional<ProjectMember> existing = memberRepository.findByProjectIdAndUserId(
			project.getId(), command.newPmUserId());
		final ProjectActor newPmBefore = existing.map(ProjectMember::getActor).orElse(null);
		existing.ifPresentOrElse(
			member -> member.changeActor(ProjectActor.PM),
			() -> memberRepository.save(ProjectMember.of(project.getId(), command.newPmUserId(), ProjectActor.PM))
		);
		memberRepository.findByProjectIdAndUserId(project.getId(), previousPmUserId).ifPresent(memberRepository::delete);
		project.changePm(command.newPmUserId());

		historyRepository.save(memberService.history(
			project, command.newPmUserId(), MemberChangeType.PM_REASSIGNED, newPmBefore, ProjectActor.PM, actor));
		historyRepository.save(memberService.history(
			project, previousPmUserId, MemberChangeType.PM_REASSIGNED, ProjectActor.PM, null, actor));
		auditLogger.record(actor, AuditAction.PROJECT_PM_REASSIGNED, AuditTargetType.PROJECT, project.getId(),
			AuditChanges.create()
				.change("pmUserId", previousPmUserId, command.newPmUserId())
				.value("addedAsMember", existing.isEmpty()));
		return resultOf(project);
	}

	/** 현 PM은 항상 PM 역할 멤버다(생성·위임·재지정이 함께 맞춘다). 없으면 데이터 불일치라 진행하지 않는다. */
	private ProjectMember getPmMember(final Long projectId, final Long pmUserId) {
		return memberRepository.findByProjectIdAndUserId(projectId, pmUserId)
			.filter(member -> member.getActor() == ProjectActor.PM)
			.orElseThrow(() -> new IllegalStateException("현재 PM이 PM 역할 멤버가 아닙니다: project=" + projectId));
	}

	private ProjectPmResult resultOf(final Project project) {
		final Long pmUserId = project.getPmUserId();
		return new ProjectPmResult(project, memberService.userNamesOf(Set.of(pmUserId)).get(pmUserId));
	}
}
