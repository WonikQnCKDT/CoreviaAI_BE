package org.wonik.coreviaai_be.domain.wbs.service.result;

import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.entity.BoardColumn;

/**
 * 보드 컬럼과 그 카드.
 *
 * @param cardCount 컬럼의 전체 카드 수(담당자 필터와 무관). WIP 초과 판정 기준
 * @param cards     표시할 카드(필터 적용). 컬럼 관리·카드 이동 응답에서는 빈 목록
 */
public record BoardColumnView(BoardColumn column, int cardCount, List<BoardCard> cards) {

	public boolean wipExceeded() {
		return column.isWipExceeded(cardCount);
	}
}
