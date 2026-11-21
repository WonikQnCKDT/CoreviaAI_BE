package org.wonik.coreviaai_be.domain.project.policy;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.service.command.InitialMember;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * 멤버·역할 규칙(FR-PRJ-04 인수 기준, 요구사항 명세서 3.2 공통 규칙). 서비스가 DB에서 읽은 값만 넘겨받아 판정하고,
 * 사용자 상태처럼 다른 테이블을 봐야 하는 검증은 서비스가 한다(ProjectCreationPolicy와 같은 나눔).
 * <ul>
 *   <li>PM은 프로젝트당 1명 이상(복수 가능). PM은 기존 멤버의 역할을 PM으로 바꿔 추가하고, 다른 지정 액터로 바꿔 해제한다.
 *       마지막 PM은 해제·제외할 수 없다.</li>
 *   <li>초대로는 PM을 줄 수 없다. 초대 후 역할 변경으로 PM을 추가한다.</li>
 *   <li>멤버 역할은 프로젝트에 지정한 액터 중에서만 고른다(PM은 필수 액터라 항상 지정돼 있다).</li>
 *   <li>게이트 승인자는 PM이 될 수 없고 멤버에서 빠질 수 없다. 승인자 변경(FR-PRJ-05)을 먼저 해야 한다.</li>
 * </ul>
 */
@Component
public class ProjectMemberPolicy {

	/** 프로젝트에 남아 있어야 하는 최소 PM 수. */
	public static final long MIN_PM_COUNT = 1L;

	/**
	 * 초대(생성 시 ⑤ 포함)로 줄 수 있는 역할인지. PM은 생성자 자동 지정이나 역할 변경으로만 정해진다.
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

	/**
	 * 역할 변경. PM 추가(→ PM)와 PM 해제(PM →)도 여기서 판정한다.
	 *
	 * @param pmCount          현재 프로젝트의 PM 수
	 * @param targetActive     대상 사용자가 활성인지(PM 추가 시에만 본다)
	 * @param targetIsApprover 대상이 게이트 승인자인지(PM 추가 시에만 본다)
	 */
	public void validateRoleChange(
		final Set<ProjectActor> assignedActors,
		final ProjectMember member,
		final ProjectActor newRole,
		final long pmCount,
		final boolean targetActive,
		final boolean targetIsApprover
	) {
		if (!assignedActors.contains(newRole)) {
			throw new CustomException(ErrorCode.MEMBER_ACTOR_NOT_ASSIGNED);
		}
		final boolean wasPm = member.getActor() == ProjectActor.PM;
		final boolean becomesPm = newRole == ProjectActor.PM;
		if (wasPm && !becomesPm) {
			requireAnotherPm(pmCount);
		}
		if (!wasPm && becomesPm) {
			if (!targetActive) {
				throw new CustomException(ErrorCode.INVALID_PM_TARGET);
			}
			if (targetIsApprover) {
				throw new CustomException(ErrorCode.PM_TARGET_IS_GATE_APPROVER);
			}
		}
	}

	/** PM은 다른 PM이 남아 있을 때만 제외할 수 있고, 게이트 승인자는 제외할 수 없다. */
	public void validateRemoval(final ProjectMember member, final boolean gateApprover, final long pmCount) {
		if (member.getActor() == ProjectActor.PM) {
			requireAnotherPm(pmCount);
		}
		if (gateApprover) {
			throw new CustomException(ErrorCode.GATE_APPROVER_NOT_REMOVABLE);
		}
	}

	private void requireAnotherPm(final long pmCount) {
		if (pmCount <= MIN_PM_COUNT) {
			throw new CustomException(ErrorCode.LAST_PM_REQUIRED);
		}
	}
}
