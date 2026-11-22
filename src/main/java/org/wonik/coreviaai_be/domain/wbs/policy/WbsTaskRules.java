package org.wonik.coreviaai_be.domain.wbs.policy;

import java.time.LocalDate;

import org.springframework.stereotype.Component;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberRepository;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 직접 등록·수정하는 WBS 항목의 값 규칙(FR-WBS-01).
 * <ul>
 *   <li>기간: 시작일 ≤ 종료일, 프로젝트 기간이 있으면 그 안(시작일·종료일 중 있는 쪽만 확인, AI 초안 반영과 같은 기준)</li>
 *   <li>담당자: 프로젝트 멤버이면서 활성 계정. 비활성(퇴사 등) 사용자에게는 새로 배정하지 않는다</li>
 * </ul>
 * 하위작업 기간은 상위 작업 기간 안으로 제한하지 않는다. 상위 작업 기간은 하위작업 기간으로 산출되기 때문이다.
 */
@Component
@RequiredArgsConstructor
public class WbsTaskRules {

	private final ProjectMemberRepository memberRepository;
	private final UserRepository userRepository;

	public void validateSchedule(final Project project, final LocalDate startDate, final LocalDate endDate) {
		if (startDate.isAfter(endDate)) {
			throw new CustomException(ErrorCode.INVALID_WBS_SCHEDULE);
		}
		if (project.getStartDate() != null && startDate.isBefore(project.getStartDate())) {
			throw new CustomException(ErrorCode.INVALID_WBS_SCHEDULE);
		}
		if (project.getEndDate() != null && endDate.isAfter(project.getEndDate())) {
			throw new CustomException(ErrorCode.INVALID_WBS_SCHEDULE);
		}
	}

	/** 담당자 미지정(null)은 허용한다. */
	public void validateAssignee(final Long projectId, final Long assigneeUserId) {
		if (assigneeUserId == null) {
			return;
		}
		final boolean activeMember = memberRepository.existsByProjectIdAndUserId(projectId, assigneeUserId)
			&& userRepository.findById(assigneeUserId).map(User::isActive).orElse(false);
		if (!activeMember) {
			throw new CustomException(ErrorCode.INVALID_TASK_ASSIGNEE);
		}
	}
}
