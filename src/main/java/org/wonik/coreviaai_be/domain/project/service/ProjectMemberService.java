package org.wonik.coreviaai_be.domain.project.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.ProjectActorAssignment;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMemberHistory;
import org.wonik.coreviaai_be.domain.project.entity.type.MemberChangeType;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.policy.ProjectAccessPolicy;
import org.wonik.coreviaai_be.domain.project.policy.ProjectMemberPolicy;
import org.wonik.coreviaai_be.domain.project.repository.ProjectActorAssignmentRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberHistoryRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectPhaseRepository;
import org.wonik.coreviaai_be.domain.project.service.command.InitialMember;
import org.wonik.coreviaai_be.domain.project.service.result.MemberHistoryResult;
import org.wonik.coreviaai_be.domain.project.service.result.ProjectMemberResult;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditChanges;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 멤버·역할 관리(FR-PRJ-04): 목록·초대·역할 변경·제외·이력. 변경은 현재 PM만 할 수 있다(3.3 권한 매트릭스).
 * <p>
 * 변경마다 업무 이력(project_member_histories)과 감사 로그(audit_logs)를 같은 트랜잭션에 남긴다.
 * 이력은 프로젝트 화면에서 보는 업무 기록이고, 감사 로그는 1년 보관하는 관리자용 기록이라 둘 다 필요하다.
 * PM 위임·재지정은 ProjectPmService가 맡는다.
 */
@Service
@RequiredArgsConstructor
public class ProjectMemberService {

	/** 업무(FR-TSK-01)가 아직 없어 담당 업무 수는 0으로 응답한다. 업무 구현 시 실제 개수로 바꾼다. */
	private static final long NO_ASSIGNED_TASKS = 0L;

	private final ProjectAccessPolicy accessPolicy;
	private final ProjectMemberPolicy memberPolicy;
	private final ProjectActorAssignmentRepository actorAssignmentRepository;
	private final ProjectMemberRepository memberRepository;
	private final ProjectMemberHistoryRepository historyRepository;
	private final ProjectPhaseRepository phaseRepository;
	private final UserRepository userRepository;
	private final AuditLogger auditLogger;
	private final Clock clock;

	/** 멤버 목록(커서, id 오름차순 = 등록순). 공개 범위는 상세와 같다. */
	@Transactional(readOnly = true)
	public CursorPage<ProjectMemberResult> findMembers(
		final Long viewerId,
		final Long projectId,
		final CursorPageRequest page
	) {
		accessPolicy.getVisibleProject(viewerId, projectId);
		final CursorPage<ProjectMember> members = CursorPage.of(
			memberRepository.findPageByProjectId(projectId, page.cursorId(), page.fetchPageable()),
			page.size(),
			ProjectMember::getId
		);
		final Map<Long, String> userNames = userNamesOf(
			members.items().stream().map(ProjectMember::getUserId).collect(Collectors.toSet()));
		return members.map(member -> new ProjectMemberResult(member, userNames.get(member.getUserId()), NO_ASSIGNED_TASKS));
	}

	/** 멤버 이력(커서, id 내림차순 = 최신순). 공개 범위는 상세와 같다. */
	@Transactional(readOnly = true)
	public CursorPage<MemberHistoryResult> findHistories(
		final Long viewerId,
		final Long projectId,
		final CursorPageRequest page
	) {
		accessPolicy.getVisibleProject(viewerId, projectId);
		final CursorPage<ProjectMemberHistory> histories = CursorPage.of(
			historyRepository.findPageByProjectId(projectId, page.cursorId(), page.fetchPageable()),
			page.size(),
			ProjectMemberHistory::getId
		);
		final Set<Long> userIds = new HashSet<>();
		histories.items().forEach(history -> {
			userIds.add(history.getUserId());
			userIds.add(history.getChangedBy());
		});
		final Map<Long, String> userNames = userNamesOf(userIds);
		return histories.map(history -> new MemberHistoryResult(
			history,
			userNames.get(history.getUserId()),
			userNames.get(history.getChangedBy())
		));
	}

	/**
	 * 여러 명을 한 번에 초대한다. 이미 멤버인 사용자가 있으면 전체를 거부한다(일부만 반영하지 않음).
	 *
	 * @return 초대한 멤버(userId 순)
	 */
	@Transactional
	public List<ProjectMemberResult> invite(final AuditActor actor, final Long projectId, final List<InitialMember> members) {
		final Project project = accessPolicy.getManagedProject(actor.userId(), projectId);
		final Set<Long> userIds = members.stream().map(InitialMember::userId).collect(Collectors.toSet());
		final List<Long> alreadyMembers = memberRepository.findAllByProjectIdAndUserIdIn(projectId, userIds).stream()
			.map(ProjectMember::getUserId)
			.toList();
		memberPolicy.validateInvite(assignedActorsOf(projectId), members, alreadyMembers);
		requireInvitableUsers(userIds);

		final List<ProjectMember> invited = members.stream()
			.sorted(Comparator.comparing(InitialMember::userId))
			.map(member -> ProjectMember.of(projectId, member.userId(), member.actor()))
			.toList();
		saveNewMembers(invited);
		historyRepository.saveAll(invited.stream()
			.map(member -> history(project, member.getUserId(), MemberChangeType.INVITED, null, member.getActor(), actor))
			.toList());

		// details 길이 제한(4000자) 안에 들도록 역할별 사용자 ID 목록으로 남긴다(최대 100명).
		final Map<ProjectActor, List<Long>> userIdsByActor = new TreeMap<>();
		invited.forEach(member ->
			userIdsByActor.computeIfAbsent(member.getActor(), key -> new ArrayList<>()).add(member.getUserId()));
		auditLogger.record(actor, AuditAction.PROJECT_MEMBER_INVITED, AuditTargetType.PROJECT, projectId,
			AuditChanges.create().value("userIdsByActor", userIdsByActor));

		final Map<Long, String> userNames = userNamesOf(userIds);
		return invited.stream()
			.map(member -> new ProjectMemberResult(member, userNames.get(member.getUserId()), NO_ASSIGNED_TASKS))
			.toList();
	}

	/** 같은 역할로 바꾸면 이력·감사 로그 없이 그대로 돌려준다. */
	@Transactional
	public ProjectMemberResult changeRole(
		final AuditActor actor,
		final Long projectId,
		final Long userId,
		final ProjectActor newRole
	) {
		final Project project = accessPolicy.getManagedProject(actor.userId(), projectId);
		final ProjectMember member = getMember(projectId, userId);
		memberPolicy.validateRoleChange(assignedActorsOf(projectId), member, newRole);
		final ProjectActor before = member.getActor();
		if (before != newRole) {
			member.changeActor(newRole);
			historyRepository.save(history(project, userId, MemberChangeType.ROLE_CHANGED, before, newRole, actor));
			auditLogger.record(actor, AuditAction.PROJECT_MEMBER_ROLE_CHANGED, AuditTargetType.PROJECT, projectId,
				AuditChanges.create().value("userId", userId).change("actor", before, newRole));
		}
		return new ProjectMemberResult(member, userNamesOf(Set.of(userId)).get(userId), NO_ASSIGNED_TASKS);
	}

	/** PM과 게이트 승인자는 제외할 수 없다(먼저 위임·승인자 변경). */
	@Transactional
	public void remove(final AuditActor actor, final Long projectId, final Long userId) {
		final Project project = accessPolicy.getManagedProject(actor.userId(), projectId);
		final ProjectMember member = getMember(projectId, userId);
		memberPolicy.validateRemoval(member, phaseRepository.existsByProjectIdAndGateApproverUserId(projectId, userId));
		memberRepository.delete(member);
		historyRepository.save(history(project, userId, MemberChangeType.REMOVED, member.getActor(), null, actor));
		auditLogger.record(actor, AuditAction.PROJECT_MEMBER_REMOVED, AuditTargetType.PROJECT, projectId,
			AuditChanges.create().value("userId", userId).value("actor", member.getActor()));
	}

	/**
	 * 멤버로 둘 수 있는 사용자인지: 활성 일반 사용자만. 경영진은 프로젝트에서 읽기 전용이라(3.1) 역할을 줄 수 없고,
	 * 시스템 관리자는 프로젝트 데이터에 접근할 수 없다. 다른 부서 사용자는 허용한다(현업 담당자 등).
	 * 생성 시 초대(FR-PRJ-01 ④)와 PM 재지정도 같은 기준을 쓴다.
	 */
	void requireInvitableUsers(final Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return;
		}
		final Set<Long> distinctIds = new HashSet<>(userIds);
		final List<User> users = userRepository.findAllById(distinctIds);
		final boolean allInvitable = users.size() == distinctIds.size()
			&& users.stream().allMatch(user -> user.isActive() && user.getSystemRole() == SystemRole.USER);
		if (!allInvitable) {
			throw new CustomException(ErrorCode.INVALID_PROJECT_MEMBER);
		}
	}

	ProjectMemberHistory history(
		final Project project,
		final Long userId,
		final MemberChangeType changeType,
		final ProjectActor before,
		final ProjectActor after,
		final AuditActor actor
	) {
		return ProjectMemberHistory.of(project.getId(), userId, changeType, before, after, actor.userId(), clock.instant());
	}

	Set<ProjectActor> assignedActorsOf(final Long projectId) {
		return actorAssignmentRepository.findAllByProjectIdOrderById(projectId).stream()
			.map(ProjectActorAssignment::getActor)
			.collect(Collectors.toSet());
	}

	Map<Long, String> userNamesOf(final Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		return userRepository.findAllById(userIds).stream()
			.collect(Collectors.toMap(User::getId, User::getName));
	}

	private ProjectMember getMember(final Long projectId, final Long userId) {
		return memberRepository.findByProjectIdAndUserId(projectId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_MEMBER_NOT_FOUND));
	}

	/** 사전 확인과 저장 사이에 같은 사용자가 먼저 초대되면 UNIQUE(project_id, user_id) 위반을 중복 오류로 바꾼다. */
	private void saveNewMembers(final List<ProjectMember> members) {
		try {
			memberRepository.saveAllAndFlush(members);
		} catch (final DataIntegrityViolationException e) {
			throw new CustomException(ErrorCode.ALREADY_PROJECT_MEMBER, e);
		}
	}
}
