package org.wonik.coreviaai_be.domain.wbs.dto.response;

import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.service.result.BoardColumnView;

/** 컬럼 관리(추가·변경·순서·삭제) 후 보드의 전체 컬럼(표시 순서). */
public record BoardColumnsResponse(List<BoardColumnSummaryResponse> columns) {

	public static BoardColumnsResponse from(final List<BoardColumnView> views) {
		return new BoardColumnsResponse(BoardColumnSummaryResponse.listOf(views));
	}
}
