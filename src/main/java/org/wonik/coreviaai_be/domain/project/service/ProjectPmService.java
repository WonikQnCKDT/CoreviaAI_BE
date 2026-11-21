package org.wonik.coreviaai_be.domain.project.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.type.MemberChangeType;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberHistoryRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectPhaseRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectRepository;
import org.wonik.coreviaai_be.domain.project.service.command.ReassignPmCommand;
import org.wonik.coreviaai_be.domain.project.service.result.NamedRef;
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
 * 시스템 관리자의 PM 재지정(FR-PRJ-04, 요구사항 명세서 3.1 예외). PM 추가·해제는 PM이 역할 변경으로 직접 하므로
 * (ProjectMemberService), 관리자는 활성 PM이 하나도 남지 않은 프로젝트(퇴사·비활성화 등)에만 새 PM을 배치할 수 있다.
 * <p>
 * 새 PM이 멤버가 아니면 PM으로 추가하고, 남아 있던 비활성 PM은 멤버에서 뺀다. 게이트 승인자는 PM이 될 수 없다.
 * 프로젝트 행을 잠그고 처리해 같은 프로젝트의 재지정·PM 변경이 겹치지 않게 한다.
 */
@Service
@RequiredArgsConstructor
public class ProjectPmService {

	private final ProjectMemberService memberService;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository memberRepository;
	private final ProjectMemberHistoryRepository historyRepository;
	private final ProjectPhaseRepository phaseRepository;
	private final UserRepository userRepository;
	private final AuditLogger auditLogger;

	/**
	 * 없는 코드와 활성 PM이 있는 프로젝트를 같은 오류(PM_REASSIGN_NOT_ALLOWED)로 알린다.
	 * 관리자는 프로젝트 데이터를 볼 수 없으므로(3.1), 코드를 바꿔 가며 프로젝트 존재 여부를 알아낼 수 없게 하기 위해서다.
	 */
	@Transactional
	public ProjectPmResult reassign(final AuditActor actor, final ReassignPmCommand command) {
		final Project project = projectRepository.findByCodeForUpdate(command.projectCode())
			.orElseThrow(() -> new CustomException(ErrorCode.PM_REASSIGN_NOT_ALLOWED));
		final List<ProjectMember> previousPms = memberRepository.findAllByProjectIdAndActorOrderById(
			project.getId(), ProjectActor.PM);
		final Map<Long, Boolean> activeByUserId = userRepository.findAllById(
				previousPms.stream().map(ProjectMember::getUserId).toList()).stream()
			.collect(Collectors.toMap(User::getId, User::isActive));
		final boolean anyActivePm = previousPms.stream()
			.anyMatch(pm -> activeByUserId.getOrDefault(pm.getUserId(), false));
		if (anyActivePm) {
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
		final List<Long> removedPmUserIds = previousPms.stream()
			.map(ProjectMember::getUserId)
			.filter(userId -> !userId.equals(command.newPmUserId()))
			.sorted()
			.toList();
		memberRepository.deleteAll(previousPms.stream()
			.filter(pm -> removedPmUserIds.contains(pm.getUserId()))
			.toList());

		historyRepository.save(memberService.history(
			project, command.newPmUserId(), MemberChangeType.PM_REASSIGNED, newPmBefore, ProjectActor.PM, actor));
		historyRepository.saveAll(removedPmUserIds.stream()
			.map(userId -> memberService.history(
				project, userId, MemberChangeType.PM_REASSIGNED, ProjectActor.PM, null, actor))
			.toList());
		auditLogger.record(actor, AuditAction.PROJECT_PM_REASSIGNED, AuditTargetType.PROJECT, project.getId(),
			AuditChanges.create()
				.value("newPmUserId", command.newPmUserId())
				.value("removedPmUserIds", removedPmUserIds)
				.value("addedAsMember", existing.isEmpty()));
		return new ProjectPmResult(project, pmsOf(project.getId()));
	}

	private List<NamedRef> pmsOf(final Long projectId) {
		final List<Long> pmUserIds = memberRepository.findAllByProjectIdAndActorOrderById(projectId, ProjectActor.PM)
			.stream()
			.map(ProjectMember::getUserId)
			.toList();
		final Map<Long, String> names = memberService.userNamesOf(pmUserIds);
		return pmUserIds.stream().map(userId -> new NamedRef(userId, names.get(userId))).toList();
	}
}
