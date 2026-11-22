package org.wonik.coreviaai_be.domain.wbs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsDependency;

public interface WbsDependencyRepository extends JpaRepository<WbsDependency, Long> {

	/** 작업이 선행이나 후행으로 걸린 관계가 있는지(작업 삭제 거부 판정). */
	boolean existsByPredecessorTaskIdOrSuccessorTaskId(Long predecessorTaskId, Long successorTaskId);
}
