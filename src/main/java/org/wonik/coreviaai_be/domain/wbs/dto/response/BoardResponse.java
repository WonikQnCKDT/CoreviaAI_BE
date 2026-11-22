package org.wonik.coreviaai_be.domain.wbs.dto.response;

import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.entity.BoardColumn;
import org.wonik.coreviaai_be.domain.wbs.service.result.BoardColumnView;

/** 칸반 보드: 컬럼(표시 순서)과 컬럼별 카드(종료일·ID 순). */
public record BoardResponse(List<ColumnResponse> columns) {

	public static BoardResponse from(final List<BoardColumnView> views) {
		return new BoardResponse(views.stream().map(ColumnResponse::from).toList());
	}

	/**
	 * 컬럼과 카드. 필드 의미는 {@link BoardColumnSummaryResponse}와 같고 cards가 더해진다.
	 * 담당자 필터를 쓰면 cards만 줄고 cardCount·wipExceeded는 컬럼 전체 기준이다.
	 */
	public record ColumnResponse(
		Long id,
		String name,
		String labelKey,
		String statusCategory,
		int sortOrder,
		Integer wipLimit,
		int cardCount,
		boolean wipExceeded,
		List<BoardCardResponse> cards
	) {

		static ColumnResponse from(final BoardColumnView view) {
			final BoardColumn column = view.column();
			return new ColumnResponse(column.getId(), column.getName(), column.defaultLabelKey(),
				column.getStatusCategory().name(), column.getSortOrder(), column.getWipLimit(), view.cardCount(),
				view.wipExceeded(), view.cards().stream().map(BoardCardResponse::from).toList());
		}
	}
}
