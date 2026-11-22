package org.wonik.coreviaai_be.domain.wbs.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.wbs.entity.BoardColumn;
import org.wonik.coreviaai_be.domain.wbs.entity.BoardLayout;
import org.wonik.coreviaai_be.domain.wbs.repository.BoardColumnRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;

import lombok.RequiredArgsConstructor;

/**
 * 보드 배치를 읽는다. 컬럼 1회 + 업무 1회 조회(N+1 없음)로 메모리에서 배치한다. 업무는 프로젝트당 최대 2,000개
 * (WbsTaskService.MAX_ITEMS_PER_PROJECT)라 WBS 트리와 같이 페이지 없이 한 번에 읽는다. 호출자 트랜잭션 안에서만 쓴다.
 */
@Component
@RequiredArgsConstructor
public class BoardReader {

	private final BoardColumnRepository columnRepository;
	private final WbsTaskRepository taskRepository;

	@Transactional(propagation = Propagation.MANDATORY)
	public BoardLayout read(final Long projectId) {
		return BoardLayout.of(columnRepository.findAllByProjectIdOrderBySortOrderAscIdAsc(projectId),
			taskRepository.findAllByProjectIdOrderBySortOrderAscIdAsc(projectId));
	}

	/**
	 * 컬럼 하나의 카드 수({@link BoardLayout#cardCount}와 같은 규칙). 보드 전체(최대 2,000개 업무)를 엔티티로 읽지 않고 COUNT 쿼리로 센다:
	 * 컬럼에 놓인 카드 + 컬럼이 그 상태의 첫 컬럼이면 컬럼이 비어 있는 같은 상태의 카드. 카드 이동·컬럼 삭제처럼 쓰기 트랜잭션에서 쓴다.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public int cardCount(final BoardColumn column) {
		final Long projectId = column.getProjectId();
		long count = taskRepository.countByProjectIdAndBoardColumnId(projectId, column.getId());
		final boolean firstOfStatus = columnRepository
			.findFirstByProjectIdAndStatusCategoryOrderBySortOrderAscIdAsc(projectId, column.getStatusCategory())
			.map(first -> first.getId().equals(column.getId()))
			.orElse(false);
		if (firstOfStatus) {
			count += taskRepository.countByProjectIdAndBoardColumnIdIsNullAndStatus(projectId, column.getStatusCategory());
		}
		return Math.toIntExact(count);
	}
}
