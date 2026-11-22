package org.wonik.coreviaai_be.domain.wbs.dto.response;

import java.time.LocalDate;
import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.entity.Milestone;
import org.wonik.coreviaai_be.domain.wbs.entity.MilestoneDeadline;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;
import org.wonik.coreviaai_be.domain.wbs.service.result.MilestoneLinkedTask;
import org.wonik.coreviaai_be.domain.wbs.service.result.MilestoneView;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 마일스톤 한 건. dDay·delayed·incompleteTaskCount는 조회 시점 계산값이다(MilestoneDeadline).
 *
 * @param dDay  오늘(Asia/Seoul)부터 목표일까지 일수. 양수 D-n, 0 D-day, 음수 D+n
 * @param tasks 연결 작업(작업 ID 순)
 */
public record MilestoneResponse(
	Long id,
	String name,
	LocalDate targetDate,
	WbsItemSource source,
	@JsonProperty("dDay") long dDay,
	boolean delayed,
	int linkedTaskCount,
	int incompleteTaskCount,
	List<LinkedTask> tasks,
	long version
) {

	public static MilestoneResponse from(final MilestoneView view) {
		final Milestone milestone = view.milestone();
		final MilestoneDeadline deadline = view.deadline();
		return new MilestoneResponse(milestone.getId(), milestone.getName(), milestone.getTargetDate(),
			milestone.getSource(), deadline.dDay(), deadline.delayed(), deadline.linkedTaskCount(),
			deadline.incompleteTaskCount(), view.tasks().stream().map(LinkedTask::from).toList(), milestone.getVersion());
	}

	public static List<MilestoneResponse> listOf(final List<MilestoneView> views) {
		return views.stream().map(MilestoneResponse::from).toList();
	}

	/** 연결 작업 요약. parentTaskId가 있으면 하위작업이다. */
	public record LinkedTask(Long id, Long parentTaskId, String name, WbsTaskStatus status, LocalDate endDate) {

		static LinkedTask from(final MilestoneLinkedTask task) {
			return new LinkedTask(task.taskId(), task.parentTaskId(), task.name(), task.status(), task.endDate());
		}
	}
}
