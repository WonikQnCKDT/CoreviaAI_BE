package org.wonik.coreviaai_be.domain.wbs.dto.response;

import org.wonik.coreviaai_be.domain.wbs.service.result.BoardCardMove;

/**
 * 카드 이동 결과.
 *
 * @param card       이동한 카드(이동 후 값, 새 version)
 * @param column     카드가 놓인 컬럼(이동 후 카드 수)
 * @param wipWarning 옮긴 컬럼의 카드 수가 WIP 한도를 넘었으면 true. 이동은 이미 반영됐고 화면에 경고만 띄운다(FR-TSK-02 인수 기준)
 */
public record BoardCardMoveResponse(BoardCardResponse card, BoardColumnSummaryResponse column, boolean wipWarning) {

	public static BoardCardMoveResponse from(final BoardCardMove move) {
		return new BoardCardMoveResponse(BoardCardResponse.from(move.card()),
			BoardColumnSummaryResponse.from(move.column()), move.column().wipExceeded());
	}
}
