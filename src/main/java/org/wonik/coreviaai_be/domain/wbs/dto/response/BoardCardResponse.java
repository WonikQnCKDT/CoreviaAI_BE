package org.wonik.coreviaai_be.domain.wbs.dto.response;

import java.time.Instant;
import java.time.LocalDate;

import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.service.result.BoardCard;

/**
 * 보드 카드(업무 = WBS 작업·하위작업).
 *
 * @param parentTaskId    하위작업이면 상위 작업 ID, 작업이면 null
 * @param parentTaskName  하위작업이면 상위 작업 이름
 * @param assignee        담당자. 없으면 null
 * @param status          진행 상태(TASK_STATUS 코드) = 놓인 컬럼의 statusCategory
 * @param statusChangedAt 마지막 상태 변경 시각(UTC). 등록 이후 바뀐 적 없으면 null
 * @param completedAt     완료 처리 시각(UTC). 완료 상태가 아니면 null
 * @param version         카드 이동·업무 수정 요청에 그대로 보내는 값(낙관적 잠금)
 */
public record BoardCardResponse(
	Long id,
	Long phaseId,
	Long parentTaskId,
	String parentTaskName,
	String name,
	String taskType,
	String priority,
	String status,
	WbsTaskResponse.AssigneeResponse assignee,
	LocalDate startDate,
	LocalDate endDate,
	int progress,
	Instant statusChangedAt,
	Instant completedAt,
	long version
) {

	public static BoardCardResponse from(final BoardCard card) {
		final WbsTask task = card.task();
		final WbsTaskResponse.AssigneeResponse assignee = task.getAssigneeUserId() == null
			? null
			: new WbsTaskResponse.AssigneeResponse(task.getAssigneeUserId(), card.assigneeName());
		return new BoardCardResponse(task.getId(), task.getPhaseId(), task.getParentTaskId(), card.parentTaskName(), task.getName(),
			task.getTaskType().name(), task.getPriority().name(), task.getStatus().name(), assignee,
			task.getStartDate(), task.getEndDate(), task.getProgress(), task.getStatusChangedAt(),
			task.getCompletedAt(), task.getVersion());
	}
}
