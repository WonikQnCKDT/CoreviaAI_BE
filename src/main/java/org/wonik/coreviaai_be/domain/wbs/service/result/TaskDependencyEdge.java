package org.wonik.coreviaai_be.domain.wbs.service.result;

/** 완료→시작 선후행 관계의 그래프 간선. */
public record TaskDependencyEdge(Long predecessorTaskId, Long successorTaskId) {
}
