package org.wonik.coreviaai_be.domain.wbs.dto.response;

import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.entity.BoardColumn;
import org.wonik.coreviaai_be.domain.wbs.service.result.BoardColumnView;

/**
 * 보드 컬럼(카드 제외). 컬럼 관리 응답과 카드 이동 응답에 쓴다.
 *
 * @param name           사용자가 붙인 이름. null이면 기본 이름이며 labelKey의 다국어 문구를 보여 준다
 * @param labelKey       기본 이름 라벨 키(code.TASK_STATUS.상태). 이름이 있으면 null
 * @param statusCategory 이 컬럼으로 옮긴 카드의 상태(TASK_STATUS 코드)
 * @param wipLimit       WIP 한도. null이면 없음
 * @param cardCount      컬럼의 전체 카드 수(필터와 무관)
 * @param wipExceeded    cardCount가 wipLimit을 넘으면 true(경고 표시, 이동은 막지 않음)
 */
public record BoardColumnSummaryResponse(
	Long id,
	String name,
	String labelKey,
	String statusCategory,
	int sortOrder,
	Integer wipLimit,
	int cardCount,
	boolean wipExceeded
) {

	public static BoardColumnSummaryResponse from(final BoardColumnView view) {
		final BoardColumn column = view.column();
		return new BoardColumnSummaryResponse(column.getId(), column.getName(), column.defaultLabelKey(),
			column.getStatusCategory().name(), column.getSortOrder(), column.getWipLimit(), view.cardCount(),
			view.wipExceeded());
	}

	public static List<BoardColumnSummaryResponse> listOf(final List<BoardColumnView> views) {
		return views.stream().map(BoardColumnSummaryResponse::from).toList();
	}
}
