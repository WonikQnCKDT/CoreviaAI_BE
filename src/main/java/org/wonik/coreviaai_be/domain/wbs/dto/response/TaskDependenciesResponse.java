package org.wonik.coreviaai_be.domain.wbs.dto.response;

import java.time.LocalDate;
import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.service.result.TaskDependencies;
import org.wonik.coreviaai_be.domain.wbs.service.result.TaskPredecessor;

/** 완료→시작 선행 관계와 현재 날짜 기준 후행 영향. */
public record TaskDependenciesResponse(Long taskId, List<PredecessorResponse> predecessors) {

	public static TaskDependenciesResponse from(final TaskDependencies dependencies) {
		return new TaskDependenciesResponse(dependencies.taskId(), dependencies.predecessors().stream()
			.map(PredecessorResponse::from)
			.toList());
	}

	public record PredecessorResponse(
		Long id,
		Long parentTaskId,
		String name,
		String status,
		LocalDate endDate,
		boolean delayed,
		long delayDays,
		boolean impactsSuccessor
	) {

		private static PredecessorResponse from(final TaskPredecessor predecessor) {
			return new PredecessorResponse(predecessor.id(), predecessor.parentTaskId(), predecessor.name(),
				predecessor.status().name(), predecessor.endDate(), predecessor.delayed(), predecessor.delayDays(),
				predecessor.impactsSuccessor());
		}
	}
}
