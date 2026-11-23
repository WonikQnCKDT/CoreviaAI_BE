package org.wonik.coreviaai_be.domain.wbs.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.service.result.WbsTreeNode;

/**
 * WBS 트리의 항목 하나. 작업이면 subtasks에 하위작업이(표시 순서대로), 하위작업이면 빈 배열이 온다.
 *
 * @param assignee        담당자. 없으면 null
 * @param scheduleDerived true면 startDate·endDate가 하위작업 기간으로 산출된 값이라 직접 바꿀 수 없다
 * @param taskType        업무 유형 TASK·DEFECT(FR-TSK-01)
 * @param priority        우선순위 HIGH·MEDIUM·LOW(FR-TSK-01)
 * @param status          진행 상태(공통 코드 TASK_STATUS). 이 API로는 바꾸지 않는다
 * @param version         수정·이동 요청에 그대로 보내는 값(낙관적 잠금)
 */
public record WbsTaskResponse(
	Long id,
	Long phaseId,
	Long parentTaskId,
	int sortOrder,
	String name,
	String description,
	AssigneeResponse assignee,
	LocalDate startDate,
	LocalDate endDate,
	boolean scheduleDerived,
	BigDecimal estimatedEffort,
	String taskType,
	String priority,
	int progress,
	String status,
	String source,
	long version,
	List<WbsTaskResponse> subtasks
) {

	public static WbsTaskResponse from(final WbsTreeNode node) {
		final WbsTask task = node.task();
		final AssigneeResponse assignee = task.getAssigneeUserId() == null
			? null
			: new AssigneeResponse(task.getAssigneeUserId(), node.assigneeName());
		return new WbsTaskResponse(task.getId(), task.getPhaseId(), task.getParentTaskId(), task.getSortOrder(), task.getName(),
			task.getDescription(), assignee, task.getStartDate(), task.getEndDate(), node.scheduleDerived(),
			task.getEstimatedEffort(), task.getTaskType().name(), task.getPriority().name(), task.getProgress(), task.getStatus().name(), task.getSource().name(),
			task.getVersion(), listOf(node.subtasks()));
	}

	public static List<WbsTaskResponse> listOf(final List<WbsTreeNode> nodes) {
		return nodes.stream().map(WbsTaskResponse::from).toList();
	}

	/** 담당자 ID·이름. 사용자를 찾지 못하면 name이 null이다. */
	public record AssigneeResponse(Long id, String name) {
	}
}
