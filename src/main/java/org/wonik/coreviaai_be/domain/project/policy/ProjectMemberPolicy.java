package org.wonik.coreviaai_be.domain.project.policy;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.type.PreviousPmAction;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.service.command.DelegatePmCommand;
import org.wonik.coreviaai_be.domain.project.service.command.InitialMember;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * 멤버·역할 규칙(FR-PRJ-04 인수 기준, 요구사항 명세서 3.2 공통 규칙). 서비스가 DB에서 읽은 값만 넘겨받아 판정하고,
 * 사용자 상태처럼 다른 테이블을 봐야 하는 검증은 서비스가 한다(ProjectCreationPolicy와 같은 나눔).
 * <ul>
 *   <li>PM은 프로젝트당 1명이고 위임·재지정으로만 바뀐다. 초대·역할 변경으로 PM을 주거나 뺄 수 없다.</li>
 *   <li>멤버 역할은 프로젝트에 지정한 액터 중에서만 고른다.</li>
 *   <li>게이트 승인자는 PM이 될 수 없고 멤버에서 빠질 수 없다. 승인자 변경(FR-PRJ-05)을 먼저 해야 한다.</li>
 * </ul>
 */
@Component
public class ProjectMemberPolicy {

	/**
	 * 초대(생성 시 ④ 포함)로 줄 수 있는 역할인지. PM은 생성자 자동 지정·위임으로만 정해진다.
	 * ProjectCreationPolicy도 같은 규칙을 쓰므로 정적 메서드로 둔다.
	 */
	public static void requireInvitableRole(final Set<ProjectActor> assignedActors, final ProjectActor role) {
		if (role == ProjectActor.PM) {
			throw new CustomException(ErrorCode.PM_ROLE_NOT_INVITABLE);
		}
		if (!assignedActors.contains(role)) {
			throw new CustomException(ErrorCode.MEMBER_ACTOR_NOT_ASSIGNED);
		}
	}

	/**
	 * @param existingMemberUserIds 초대 대상 중 이미 멤버인 사용자
	 */
	public void validateInvite(
		final Set<ProjectActor> assignedActors,
		final List<InitialMember> members,
		final Collection<Long> existingMemberUserIds
	) {
		final Set<Long> userIds = new HashSet<>();
		for (final InitialMember member : members) {
			requireInvitableRole(assignedActors, member.actor());
			if (!userIds.add(member.userId())) {
				throw new CustomException(ErrorCode.DUPLICATE_PROJECT_MEMBER);
			}
		}
		if (!existingMemberUserIds.isEmpty()) {
			throw new CustomException(ErrorCode.ALREADY_PROJECT_MEMBER);
		}
	}

	/** PM의 역할을 바꾸거나 누군가를 PM으로 바꾸는 것은 위임으로만 한다. 게이트 승인자의 역할 변경은 허용한다. */
	public void validateRoleChange(
		final Set<ProjectActor> assignedActors,
		final ProjectMember member,
		final ProjectActor newRole
	) {
		if (member.getActor() == ProjectActor.PM || newRole == ProjectActor.PM) {
			throw new CustomException(ErrorCode.PM_ROLE_NOT_CHANGEABLE);
		}
		if (!assignedActors.contains(newRole)) {
			throw new CustomException(ErrorCode.MEMBER_ACTOR_NOT_ASSIGNED);
		}
	}

	public void validateRemoval(final ProjectMember member, final boolean gateApprover) {
		if (member.getActor() == ProjectActor.PM) {
			throw new CustomException(ErrorCode.CANNOT_REMOVE_PM);
		}
		if (gateApprover) {
			throw new CustomException(ErrorCode.GATE_APPROVER_NOT_REMOVABLE);
		}
	}

	/**
	 * @param target           위임 대상 멤버. 멤버가 아니면 null
	 * @param targetActive     위임 대상 사용자가 활성인지
	 * @param targetIsApprover 위임 대상이 게이트 승인자인지
	 */
	public void validateDelegation(
		final Set<ProjectActor> assignedActors,
		final Long currentPmUserId,
		final DelegatePmCommand command,
		final ProjectMember target,
		final boolean targetActive,
		final boolean targetIsApprover
	) {
		if (target == null || target.getUserId().equals(currentPmUserId) || !targetActive) {
			throw new CustomException(ErrorCode.INVALID_PM_DELEGATION_TARGET);
		}
		if (targetIsApprover) {
			throw new CustomException(ErrorCode.PM_TARGET_IS_GATE_APPROVER);
		}
		validatePreviousPmAction(assignedActors, command.previousPmAction(), command.previousPmActor());
	}

	/** 남은 지정 액터 중 하나로 바꾸거나(CHANGE_ROLE), 역할 없이 멤버에서 빠진다(LEAVE). */
	private void validatePreviousPmAction(
		final Set<ProjectActor> assignedActors,
		final PreviousPmAction action,
		final ProjectActor previousPmActor
	) {
		if (action == PreviousPmAction.LEAVE) {
			if (previousPmActor != null) {
				throw new CustomException(ErrorCode.INVALID_PREVIOUS_PM_ACTION);
			}
			return;
		}
		if (previousPmActor == null || previousPmActor == ProjectActor.PM) {
			throw new CustomException(ErrorCode.INVALID_PREVIOUS_PM_ACTION);
		}
		if (!assignedActors.contains(previousPmActor)) {
			throw new CustomException(ErrorCode.MEMBER_ACTOR_NOT_ASSIGNED);
		}
	}
}
