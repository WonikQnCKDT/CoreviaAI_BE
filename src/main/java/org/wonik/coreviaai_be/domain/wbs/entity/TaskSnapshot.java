package org.wonik.coreviaai_be.domain.wbs.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskPriority;
import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskType;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

/**
 * 변경 이력을 남기기 위해 바꾸기 전·후에 떠 두는 작업 값. 엔티티는 같은 객체가 바뀌므로 변경 전에 이 값을 먼저 만든다.
 */
public record TaskSnapshot(
	Long taskId,
	Long parentTaskId,
	int sortOrder,
	String name,
	String description,
	Long assigneeUserId,
	LocalDate startDate,
	LocalDate endDate,
	BigDecimal estimatedEffort,
	TaskType taskType,
	TaskPriority priority,
	String completionCriteria,
	WbsTaskStatus status,
	WbsItemSource source
) {

	public static TaskSnapshot of(final WbsTask task) {
		return new TaskSnapshot(task.getId(), task.getParentTaskId(), task.getSortOrder(), task.getName(),
			task.getDescription(), task.getAssigneeUserId(), task.getStartDate(), task.getEndDate(),
			task.getEstimatedEffort(), task.getTaskType(), task.getPriority(), task.getCompletionCriteria(),
			task.getStatus(), task.getSource());
	}
}
