package org.wonik.coreviaai_be.domain.wbs.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

public interface WbsTaskRepository extends JpaRepository<WbsTask, Long> {

	/** 최상위 작업을 표시 순서대로. 하위작업은 제외한다. */
	List<WbsTask> findAllByProjectIdAndParentTaskIdIsNullOrderBySortOrderAscIdAsc(Long projectId);

	/** 최상위 작업의 마지막 표시 순서. 작업이 없으면 0. 새 작업을 뒤에 붙일 때 쓴다. */
	@Query("select coalesce(max(t.sortOrder), 0) from WbsTask t where t.projectId = :projectId and t.parentTaskId is null")
	int findLastTopLevelSortOrder(@Param("projectId") Long projectId);

	/** 상위 작업 안 하위작업의 마지막 표시 순서. 하위작업이 없으면 0. */
	@Query("select coalesce(max(t.sortOrder), 0) from WbsTask t where t.parentTaskId = :parentTaskId")
	int findLastSortOrderUnder(@Param("parentTaskId") Long parentTaskId);

	/** 다른 프로젝트의 작업 ID로 접근하지 못하도록 항상 프로젝트와 함께 찾는다. */
	Optional<WbsTask> findByIdAndProjectId(Long id, Long projectId);

	/** 프로젝트 WBS 전체(작업·하위작업). 트리는 이 한 번의 조회 결과로 메모리에서 조립한다(N+1 방지). */
	List<WbsTask> findAllByProjectIdOrderBySortOrderAscIdAsc(Long projectId);

	List<WbsTask> findAllByParentTaskIdOrderBySortOrderAscIdAsc(Long parentTaskId);

	boolean existsByParentTaskId(Long parentTaskId);

	/** 프로젝트당 WBS 항목 수 상한 판정용. */
	long countByProjectId(Long projectId);

	/** 주어진 ID 중 그 프로젝트의 항목 수. 마일스톤 연결 작업이 모두 같은 프로젝트에 있는지 확인한다. */
	long countByProjectIdAndIdIn(Long projectId, Collection<Long> ids);

	/** 컬럼에 놓인 카드 수(idx_wbs_tasks_board_column_id). 카드 이동 WIP 판정·컬럼 삭제 판정에 쓴다. */
	long countByProjectIdAndBoardColumnId(Long projectId, Long boardColumnId);

	/** 컬럼이 비어 있는(board_column_id NULL) 그 상태의 카드 수. 이런 카드는 같은 상태의 첫 컬럼에 보인다(BoardLayout). */
	long countByProjectIdAndBoardColumnIdIsNullAndStatus(Long projectId, WbsTaskStatus status);

	/**
	 * 표시 순서만 바꾼다. 순서는 프로젝트 행 잠금으로 보호하는 구조 정보라 version을 올리지 않는다
	 * (형제 작업을 끌어다 놓아도 그 작업을 고치던 다른 사용자가 409를 받지 않게). 영속성 컨텍스트는 비운다.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update WbsTask t set t.sortOrder = :sortOrder where t.id = :id")
	int updateSortOrder(@Param("id") Long id, @Param("sortOrder") int sortOrder);
}
