package org.wonik.coreviaai_be.domain.wbs.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.service.result.TaskDetail;

/**
 * 업무 상세(FR-TSK-01). WBS 트리 항목의 필드에 완료 조건·완료 시각·등록자·생성·수정 시각을 더한다.
 *
 * @param assignee        담당자. 없으면 null
 * @param scheduleDerived true면 기간이 하위작업 기간으로 산출된 값이라 직접 바꿀 수 없다
 * @param completedAt     완료 처리 시각(UTC). 완료 상태가 아니면 null(칸반 카드 이동 FR-TSK-02가 기록)
 * @param boardColumnId   칸반 컬럼. null이면 같은 상태의 첫 컬럼에 놓인다
 * @param statusChangedAt 마지막 상태 변경 시각(UTC). 등록 이후 바뀐 적 없으면 null
 * @param version         상세 필드 수정·WBS 기본 필드 수정 요청에 그대로 보내는 값(낙관적 잠금)
 */
public record TaskDetailResponse(
	Long id,
	Long projectId,
	Long phaseId,
	Long parentTaskId,
	String name,
	String description,
	String taskType,
	String priority,
	String completionCriteria,
	WbsTaskResponse.AssigneeResponse assignee,
	LocalDate startDate,
	LocalDate endDate,
	boolean scheduleDerived,
	BigDecimal estimatedEffort,
	int progress,
	String status,
	String source,
	Instant completedAt,
	Long boardColumnId,
	Instant statusChangedAt,
	UserSummary createdBy,
	Instant createdAt,
	Instant updatedAt,
	long version
) {

	public static TaskDetailResponse from(final TaskDetail detail) {
		final WbsTask task = detail.task();
		final WbsTaskResponse.AssigneeResponse assignee = task.getAssigneeUserId() == null
			? null
			: new WbsTaskResponse.AssigneeResponse(task.getAssigneeUserId(), detail.assigneeName());
		return new TaskDetailResponse(task.getId(), task.getProjectId(), task.getPhaseId(), task.getParentTaskId(),
			task.getName(),
			task.getDescription(), task.getTaskType().name(), task.getPriority().name(), task.getCompletionCriteria(),
			assignee, task.getStartDate(), task.getEndDate(), detail.scheduleDerived(), task.getEstimatedEffort(),
			task.getProgress(), task.getStatus().name(), task.getSource().name(), task.getCompletedAt(),
			task.getBoardColumnId(), task.getStatusChangedAt(),
			new UserSummary(task.getCreatedBy(), detail.createdByName()), task.getCreatedAt(), task.getUpdatedAt(),
			task.getVersion());
	}

	/** 사용자 ID·이름. 사용자를 찾지 못하면 name이 null이다. */
	public record UserSummary(Long id, String name) {
	}
}
