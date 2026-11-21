package org.wonik.coreviaai_be.domain.wbs.service.result;

/**
 * 카드 이동 결과.
 *
 * @param card   이동한 카드(이동 후 값)
 * @param column 카드가 놓인 컬럼과 이동 후 카드 수. column.wipExceeded()가 true면 WIP 한도 초과 경고
 */
public record BoardCardMove(BoardCard card, BoardColumnView column) {
}
