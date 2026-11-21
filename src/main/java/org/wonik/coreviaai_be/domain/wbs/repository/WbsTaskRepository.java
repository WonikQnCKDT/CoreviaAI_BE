package org.wonik.coreviaai_be.domain.wbs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;

public interface WbsTaskRepository extends JpaRepository<WbsTask, Long> {

	/** 최상위 작업을 표시 순서대로. 하위작업은 제외한다. */
	List<WbsTask> findAllByProjectIdAndParentTaskIdIsNullOrderBySortOrderAscIdAsc(Long projectId);

	/** 최상위 작업의 마지막 표시 순서. 작업이 없으면 0. 새 작업을 뒤에 붙일 때 쓴다. */
	@Query("select coalesce(max(t.sortOrder), 0) from WbsTask t where t.projectId = :projectId and t.parentTaskId is null")
	int findLastTopLevelSortOrder(@Param("projectId") Long projectId);
}
