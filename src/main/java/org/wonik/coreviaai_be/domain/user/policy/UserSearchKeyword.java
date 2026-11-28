package org.wonik.coreviaai_be.domain.user.policy;

import java.util.Locale;

import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * 일반 사용자용 이름 검색어(GET /api/users/search). 앞뒤 공백을 뺀 2~50자(문자 단위)만 받고 제어 문자는 거부한다.
 * <p>
 * 최소 길이는 한두 글자로 직원 목록 전체를 훑는 비용을 높이기 위한 것이고, 최대 길이는 users.name 컬럼 길이(50)에 맞춘다.
 * 검색어는 개인정보 조회 조건이라 예외 메시지·로그에 값을 넣지 않는다.
 *
 * @param value 앞뒤 공백을 뺀 검색어
 */
public record UserSearchKeyword(String value) {

	public static final int MIN_LENGTH = 2;
	public static final int MAX_LENGTH = 50;

	public UserSearchKeyword {
		if (value == null) {
			throw new CustomException(ErrorCode.INVALID_USER_SEARCH_KEYWORD);
		}
		final int length = value.codePointCount(0, value.length());
		if (length < MIN_LENGTH || length > MAX_LENGTH || containsControl(value) || !value.equals(value.strip())) {
			throw new CustomException(ErrorCode.INVALID_USER_SEARCH_KEYWORD);
		}
	}

	/** 요청 값을 정규화한다(앞뒤 공백 제거). null이거나 규칙에 맞지 않으면 INVALID_USER_SEARCH_KEYWORD(400). */
	public static UserSearchKeyword of(final String raw) {
		if (raw == null) {
			throw new CustomException(ErrorCode.INVALID_USER_SEARCH_KEYWORD);
		}
		return new UserSearchKeyword(raw.strip());
	}

	/**
	 * 이름 부분 일치용 LIKE 패턴(소문자, 이스케이프 문자는 역슬래시). 사용자가 입력한 %, _, 역슬래시가 와일드카드로
	 * 해석되지 않고 문자 그대로 검색되게 한다. 파라미터 바인딩으로만 쓴다.
	 */
	public String likePattern() {
		final String escaped = value.toLowerCase(Locale.ROOT)
			.replace("\\", "\\\\")
			.replace("%", "\\%")
			.replace("_", "\\_");
		return "%" + escaped + "%";
	}

	private static boolean containsControl(final String value) {
		return value.codePoints().anyMatch(Character::isISOControl);
	}

	/** 검색어가 로그·예외 메시지로 새지 않도록 값을 감춘다. */
	@Override
	public String toString() {
		return "UserSearchKeyword[length=" + value.codePointCount(0, value.length()) + "]";
	}
}
