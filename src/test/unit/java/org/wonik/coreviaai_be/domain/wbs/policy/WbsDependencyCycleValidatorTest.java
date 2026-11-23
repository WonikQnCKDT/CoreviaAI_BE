package org.wonik.coreviaai_be.domain.wbs.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.wbs.service.result.TaskDependencyEdge;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

class WbsDependencyCycleValidatorTest {

	private final WbsDependencyCycleValidator validator = new WbsDependencyCycleValidator();

	@Test
	@DisplayName("분기·합류가 있는 비순환 완료→시작 그래프는 허용한다")
	void acceptsDirectedAcyclicGraph() {
		assertThatCode(() -> validator.validate(List.of(
			new TaskDependencyEdge(1L, 2L),
			new TaskDependencyEdge(1L, 3L),
			new TaskDependencyEdge(2L, 4L),
			new TaskDependencyEdge(3L, 4L))))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("자기 자신을 선행 작업으로 연결하면 순환으로 거부한다")
	void rejectsSelfCycle() {
		expectCycle(List.of(new TaskDependencyEdge(1L, 1L)));
	}

	@Test
	@DisplayName("A→B→C→A 간접 순환을 거부한다")
	void rejectsIndirectCycle() {
		expectCycle(List.of(
			new TaskDependencyEdge(1L, 2L),
			new TaskDependencyEdge(2L, 3L),
			new TaskDependencyEdge(3L, 1L)));
	}

	@Test
	@DisplayName("중복 간선은 한 번만 계산한다")
	void ignoresDuplicateEdges() {
		assertThatCode(() -> validator.validate(List.of(
			new TaskDependencyEdge(1L, 2L),
			new TaskDependencyEdge(1L, 2L),
			new TaskDependencyEdge(2L, 3L))))
			.doesNotThrowAnyException();
	}

	private void expectCycle(final List<TaskDependencyEdge> edges) {
		assertThatThrownBy(() -> validator.validate(edges))
			.isInstanceOf(CustomException.class)
			.extracting(error -> ((CustomException)error).getErrorCode())
			.isEqualTo(ErrorCode.WBS_DEPENDENCY_CYCLE);
	}
}
