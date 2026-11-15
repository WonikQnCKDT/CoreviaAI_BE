package org.wonik.coreviaai_be.domain.project.policy;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.service.command.CreateProjectCommand;
import org.wonik.coreviaai_be.domain.project.service.command.InitialMember;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * 프로젝트 생성 규칙(FR-PRJ-01 인수 기준). 명령과 생성자 ID만으로 판정할 수 있는 규칙만 다루고,
 * DB를 봐야 하는 검증(코드 중복, 사용자·부서 상태)은 서비스가 한다.
 * FE가 비활성 항목을 회색으로 막더라도 서버에서 같은 규칙으로 다시 거부한다.
 */
@Component
public class ProjectCreationPolicy {

	/** @param creatorUserId 생성자. PM으로 자동 등록되므로 초대 멤버·게이트 승인자가 될 수 없다 */
	public void validate(final CreateProjectCommand command, final Long creatorUserId) {
		validatePeriod(command);
		validateMethodology(command);
		validateActors(command);
		validateMembers(command, creatorUserId);
		validateGateApprover(command, creatorUserId);
	}

	private void validatePeriod(final CreateProjectCommand command) {
		if (command.startDate().isAfter(command.endDate())) {
			throw new CustomException(ErrorCode.INVALID_PROJECT_PERIOD);
		}
	}

	private void validateMethodology(final CreateProjectCommand command) {
		if (!command.methodology().isEnabled()) {
			throw new CustomException(ErrorCode.METHODOLOGY_NOT_AVAILABLE);
		}
	}

	/** PM은 필수 액터이고, 나머지는 선택한 부서 유형에서 선택 가능한 액터여야 한다. */
	private void validateActors(final CreateProjectCommand command) {
		if (!command.departmentType().isEnabled()) {
			throw new CustomException(ErrorCode.DEPARTMENT_TYPE_NOT_AVAILABLE);
		}
		if (!command.actors().contains(ProjectActor.PM)) {
			throw new CustomException(ErrorCode.PM_ACTOR_REQUIRED);
		}
		for (final ProjectActor actor : command.actors()) {
			if (!actor.selectableFor(command.departmentType())) {
				throw new CustomException(ErrorCode.ACTOR_NOT_AVAILABLE);
			}
		}
	}

	/** 멤버 역할은 지정한 액터 중에서만 고르고, PM 역할은 생성자 자동 등록이라 초대로 줄 수 없다. */
	private void validateMembers(final CreateProjectCommand command, final Long creatorUserId) {
		final Set<Long> userIds = new HashSet<>();
		for (final InitialMember member : command.members()) {
			if (Objects.equals(member.userId(), creatorUserId)) {
				throw new CustomException(ErrorCode.CREATOR_ALREADY_PM);
			}
			if (member.actor() == ProjectActor.PM) {
				throw new CustomException(ErrorCode.PM_ROLE_NOT_INVITABLE);
			}
			if (!command.actors().contains(member.actor())) {
				throw new CustomException(ErrorCode.MEMBER_ACTOR_NOT_ASSIGNED);
			}
			if (!userIds.add(member.userId())) {
				throw new CustomException(ErrorCode.DUPLICATE_PROJECT_MEMBER);
			}
		}
	}

	/** 게이트 승인자는 PM(생성자)이 아닌, 초대한 멤버 중에서만 지정한다(FR-PRJ-05). */
	private void validateGateApprover(final CreateProjectCommand command, final Long creatorUserId) {
		final Long approverId = command.gateApproverUserId();
		if (approverId == null) {
			return;
		}
		if (Objects.equals(approverId, creatorUserId)) {
			throw new CustomException(ErrorCode.GATE_APPROVER_CANNOT_BE_PM);
		}
		final boolean invited = command.members().stream()
			.anyMatch(member -> Objects.equals(member.userId(), approverId));
		if (!invited) {
			throw new CustomException(ErrorCode.GATE_APPROVER_NOT_MEMBER);
		}
	}
}
