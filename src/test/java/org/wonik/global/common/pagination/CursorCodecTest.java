package org.wonik.global.common.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

class CursorCodecTest {

	@Test
	@DisplayName("인코딩한 커서를 디코딩하면 같은 id가 나온다")
	void roundTrip() {
		assertThat(CursorCodec.decode(CursorCodec.encode(42L))).isEqualTo(42L);
		assertThat(CursorCodec.decode(CursorCodec.encode(Long.MAX_VALUE))).isEqualTo(Long.MAX_VALUE);
	}

	@Test
	@DisplayName("커서가 없으면 첫 페이지(null)")
	void emptyCursorMeansFirstPage() {
		assertThat(CursorCodec.decode(null)).isNull();
		assertThat(CursorCodec.decode(" ")).isNull();
	}

	@ParameterizedTest
	@ValueSource(strings = {"not-base64!", "djE6YWJj", "MTIz", "djE6MA", "djE6LTE"})
	@DisplayName("형식이 틀리거나(접두어 없음·숫자 아님) id가 양수가 아니면 INVALID_CURSOR")
	void invalidCursor(final String cursor) {
		// djE6YWJj = "v1:abc", MTIz = "123", djE6MA = "v1:0", djE6LTE = "v1:-1"
		assertThatThrownBy(() -> CursorCodec.decode(cursor))
			.isInstanceOf(CustomException.class)
			.extracting(e -> ((CustomException)e).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_CURSOR);
	}

	@Test
	@DisplayName("테스트에 쓴 커서 문자열이 실제 인코딩과 같다")
	void fixturesMatchEncoding() {
		final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
		assertThat(encoder.encodeToString("v1:abc".getBytes(StandardCharsets.UTF_8))).isEqualTo("djE6YWJj");
		assertThat(encoder.encodeToString("v1:0".getBytes(StandardCharsets.UTF_8))).isEqualTo("djE6MA");
		assertThat(encoder.encodeToString("v1:-1".getBytes(StandardCharsets.UTF_8))).isEqualTo("djE6LTE");
	}
}
