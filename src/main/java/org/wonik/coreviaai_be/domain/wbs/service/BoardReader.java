package org.wonik.coreviaai_be.domain.wbs.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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
}
