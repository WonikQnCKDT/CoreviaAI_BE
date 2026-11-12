package org.wonik.global.common.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * 커서 목록 요청. cursorId가 null이면 첫 페이지다. 정렬 방향은 각 목록 쿼리가 정하고 커서는 정렬 키(id)만 담는다.
 */
public record CursorPageRequest(Long cursorId, int size) {

	public static final int DEFAULT_SIZE = 20;
	public static final int MAX_SIZE = 100;

	public static CursorPageRequest of(final String cursor, final int size) {
		return new CursorPageRequest(CursorCodec.decode(cursor), size);
	}

	/** 다음 페이지 존재 여부를 알기 위해 size보다 1개 더 조회한다({@link CursorPage#of}). 정렬은 쿼리에 고정돼 있어 개수만 담는다. */
	public Pageable fetchPageable() {
		return PageRequest.of(0, size + 1);
	}
}
