package org.wonik.coreviaai_be.domain.wbs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsDependency;

public interface WbsDependencyRepository extends JpaRepository<WbsDependency, Long> {

	/** 작업이 선행이나 후행으로 걸린 관계가 있는지(작업 삭제 거부 판정). */
	boolean existsByPredecessorTaskIdOrSuccessorTaskId(Long predecessorTaskId, Long successorTaskId);

	/** 프로젝트의 전체 관계. 선후행 교체 전에 전체 그래프의 순환을 한 번에 검증한다. */
	List<WbsDependency> findAllByProjectIdOrderByIdAsc(Long projectId);

	/** 후행 작업에 연결된 선행 작업. 응답 순서를 안정적으로 유지한다. */
	List<WbsDependency> findAllBySuccessorTaskIdOrderByPredecessorTaskIdAsc(Long successorTaskId);
}
