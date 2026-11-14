package org.wonik.global.common.pagination;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * 커서 문자열과 정렬 키(id) 사이의 변환. 클라이언트는 커서를 해석하지 않고 그대로 돌려보낸다(불투명 값).
 * <p>
 * 형식: Base64URL("v1:" + id). 접두어로 형식 버전을 남겨, 정렬 키가 바뀌어도 이전 커서를 구분해 거부할 수 있게 한다.
 */
public final class CursorCodec {

	private static final String PREFIX = "v1:";

	private CursorCodec() {
	}

	public static String encode(final long id) {
		return Base64.getUrlEncoder().withoutPadding()
			.encodeToString((PREFIX + id).getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * @return 커서가 없으면(null·빈 문자열) null — 첫 페이지
	 * @throws CustomException INVALID_CURSOR 형식이 맞지 않거나 id가 양수가 아닐 때
	 */
	public static Long decode(final String cursor) {
		if (cursor == null || cursor.isBlank()) {
			return null;
		}
		try {
			final String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
			if (!decoded.startsWith(PREFIX)) {
				throw new CustomException(ErrorCode.INVALID_CURSOR);
			}
			final long id = Long.parseLong(decoded.substring(PREFIX.length()));
			if (id <= 0) {
				throw new CustomException(ErrorCode.INVALID_CURSOR);
			}
			return id;
		} catch (final IllegalArgumentException e) {
			// Base64 형식 오류와 NumberFormatException(IllegalArgumentException 하위)을 함께 처리한다.
			throw new CustomException(ErrorCode.INVALID_CURSOR);
		}
	}
}
