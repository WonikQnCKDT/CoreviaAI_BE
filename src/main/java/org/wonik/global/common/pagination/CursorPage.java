package org.wonik.global.common.pagination;

import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * 커서 방식 목록 한 페이지. 서비스는 도메인 결과로 만들고, 컨트롤러는 {@link #map(Function)}으로 응답 DTO로 바꿔 그대로 응답한다.
 *
 * @param items      이번 페이지 항목(최대 size개)
 * @param nextCursor 다음 페이지 요청에 그대로 넣을 값. hasNext가 false면 null
 * @param hasNext    다음 페이지 존재 여부
 */
public record CursorPage<T>(List<T> items, String nextCursor, boolean hasNext) {

	/**
	 * size + 1개까지 조회한 결과로 페이지를 만든다. 1개를 더 읽어 다음 페이지 존재 여부를 count 쿼리 없이 판단한다.
	 *
	 * @param fetched 정렬 키(id) 순서로 조회한 결과(size + 1개 이하)
	 * @param idOf    정렬 키 추출. 마지막 항목의 키가 다음 커서가 된다
	 */
	public static <T> CursorPage<T> of(final List<T> fetched, final int size, final ToLongFunction<T> idOf) {
		final boolean hasNext = fetched.size() > size;
		final List<T> page = hasNext ? List.copyOf(fetched.subList(0, size)) : List.copyOf(fetched);
		final String nextCursor = hasNext ? CursorCodec.encode(idOf.applyAsLong(page.get(page.size() - 1))) : null;
		return new CursorPage<>(page, nextCursor, hasNext);
	}

	public <R> CursorPage<R> map(final Function<T, R> mapper) {
		return new CursorPage<>(items.stream().map(mapper).toList(), nextCursor, hasNext);
	}
}
