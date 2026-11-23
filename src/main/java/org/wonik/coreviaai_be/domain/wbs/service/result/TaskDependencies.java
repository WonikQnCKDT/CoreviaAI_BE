package org.wonik.coreviaai_be.domain.wbs.service.result;

import java.util.List;

/** 후행 작업 하나의 완료→시작 선행 관계 목록. */
public record TaskDependencies(Long taskId, List<TaskPredecessor> predecessors) {

	public TaskDependencies {
		predecessors = List.copyOf(predecessors);
	}
}
