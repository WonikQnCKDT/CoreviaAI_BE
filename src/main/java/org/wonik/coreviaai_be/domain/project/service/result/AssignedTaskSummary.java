package org.wonik.coreviaai_be.domain.project.service.result;

import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

/** 멤버 제외를 막은 미완료 담당 업무. FE가 재배정 화면으로 연결할 최소 정보만 제공한다. */
public record AssignedTaskSummary(Long taskId, String title, WbsTaskStatus status) {

	public static AssignedTaskSummary from(final WbsTask task) {
		return new AssignedTaskSummary(task.getId(), task.getName(), task.getStatus());
	}
}
