package org.wonik.coreviaai_be.domain.wbs.policy;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.wonik.coreviaai_be.domain.wbs.service.result.TaskDependencyEdge;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/** 완료→시작 관계가 방향성 비순환 그래프인지 Kahn 위상 정렬로 검증한다. */
@Component
public class WbsDependencyCycleValidator {

	public void validate(final Collection<TaskDependencyEdge> edges) {
		final Map<Long, Set<Long>> successors = new HashMap<>();
		final Map<Long, Integer> indegrees = new HashMap<>();
		for (final TaskDependencyEdge edge : edges) {
			indegrees.putIfAbsent(edge.predecessorTaskId(), 0);
			indegrees.putIfAbsent(edge.successorTaskId(), 0);
			if (successors.computeIfAbsent(edge.predecessorTaskId(), ignored -> new HashSet<>())
				.add(edge.successorTaskId())) {
				indegrees.compute(edge.successorTaskId(), (ignored, degree) -> degree + 1);
			}
		}

		final Queue<Long> ready = new ArrayDeque<>();
		indegrees.forEach((taskId, degree) -> {
			if (degree == 0) {
				ready.add(taskId);
			}
		});
		int visited = 0;
		while (!ready.isEmpty()) {
			final Long taskId = ready.remove();
			visited++;
			for (final Long successor : successors.getOrDefault(taskId, Set.of())) {
				final int remaining = indegrees.compute(successor, (ignored, degree) -> degree - 1);
				if (remaining == 0) {
					ready.add(successor);
				}
			}
		}
		if (visited != indegrees.size()) {
			throw new CustomException(ErrorCode.WBS_DEPENDENCY_CYCLE);
		}
	}
}
