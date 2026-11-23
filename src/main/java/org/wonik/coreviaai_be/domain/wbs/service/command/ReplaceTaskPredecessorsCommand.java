package org.wonik.coreviaai_be.domain.wbs.service.command;

import java.util.Set;

/** 후행 작업에 연결할 선행 작업 전체 집합. 빈 집합이면 모두 해제한다. */
public record ReplaceTaskPredecessorsCommand(Set<Long> predecessorTaskIds) {

	public ReplaceTaskPredecessorsCommand {
		predecessorTaskIds = Set.copyOf(predecessorTaskIds);
	}
}
