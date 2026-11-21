package org.wonik.coreviaai_be.domain.wbs.dto.request;

/**
 * 반영 요청의 선택 입력 문자열 정규화. 공백뿐인 값은 입력하지 않은 것(null)으로 본다.
 */
final class DraftText {

	private DraftText() {
	}

	static String trimToNull(final String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}
}
