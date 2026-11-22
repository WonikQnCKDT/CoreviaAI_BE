package org.wonik.coreviaai_be.domain.wbs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.wbs.entity.BoardColumn;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, Long> {

	/** 보드 컬럼을 표시 순서대로(idx_board_columns_project_id). */
	List<BoardColumn> findAllByProjectIdOrderBySortOrderAscIdAsc(Long projectId);

	/** 다른 프로젝트의 컬럼 ID로 접근하지 못하도록 항상 프로젝트와 함께 찾는다. */
	Optional<BoardColumn> findByIdAndProjectId(Long id, Long projectId);

	Optional<BoardColumn> findFirstByProjectIdAndStatusCategoryOrderBySortOrderAscIdAsc(
		Long projectId, WbsTaskStatus statusCategory);

	long countByProjectId(Long projectId);

	long countByProjectIdAndStatusCategory(Long projectId, WbsTaskStatus statusCategory);

	/** 보드 안 마지막 표시 순서. 컬럼이 없으면 0. 새 컬럼을 맨 뒤에 붙일 때 쓴다. */
	@Query("select coalesce(max(c.sortOrder), 0) from BoardColumn c where c.projectId = :projectId")
	int findLastSortOrder(@Param("projectId") Long projectId);

	/**
	 * 새 작업(상태 TODO)을 놓을 컬럼: 대기(TODO) 상태 컬럼 중 가장 앞. 컬럼이 없으면 null(조회 시 같은 규칙으로 배치된다).
	 */
	default Long findInitialColumnId(final Long projectId) {
		return findFirstByProjectIdAndStatusCategoryOrderBySortOrderAscIdAsc(projectId, WbsTaskStatus.TODO)
			.map(BoardColumn::getId)
			.orElse(null);
	}
}
