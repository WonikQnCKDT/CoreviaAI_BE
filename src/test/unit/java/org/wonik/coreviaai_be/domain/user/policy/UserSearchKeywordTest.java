package org.wonik.coreviaai_be.domain.user.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

class UserSearchKeywordTest {

	@Test
	@DisplayName("앞뒤 공백을 빼고 2자·50자 경계를 허용한다")
	void normalizesAndAcceptsBoundaries() {
		assertThat(UserSearchKeyword.of("  홍길  ").value()).isEqualTo("홍길");
		assertThat(UserSearchKeyword.of("ab").value()).isEqualTo("ab");
		assertThat(UserSearchKeyword.of("a".repeat(50)).value()).hasSize(50);
		// 공백을 뺀 뒤 길이를 본다
		assertThat(UserSearchKeyword.of(" " + "a".repeat(50) + " ").value()).hasSize(50);
		// 가운데 공백은 유지한다
		assertThat(UserSearchKeyword.of("홍 길동").value()).isEqualTo("홍 길동");
	}

	@Test
	@DisplayName("길이는 문자(code point) 단위로 센다")
	void countsCodePoints() {
		// 이모지 2개는 UTF-16 길이 4지만 2자다
		assertThat(UserSearchKeyword.of("😀😀").value()).isNotNull();
		final String fiftyOneEmoji = "😀".repeat(51);
		expectInvalid(fiftyOneEmoji);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " ", "a", " a ", "\t\n", "a\u0000b", "ab\u0007", "a\u007Fb", "a\u0085b"})
	@DisplayName("빈 값·1자·공백만·제어 문자는 거부한다")
	void rejectsInvalid(final String raw) {
		expectInvalid(raw);
	}

	@Test
	@DisplayName("null과 51자는 거부한다")
	void rejectsNullAndTooLong() {
		expectInvalid(null);
		expectInvalid("a".repeat(51));
	}

	@Test
	@DisplayName("LIKE 패턴은 소문자이고 %, _, 역슬래시를 문자 그대로 매칭하도록 이스케이프한다")
	void escapesWildcards() {
		assertThat(UserSearchKeyword.of("Kim").likePattern()).isEqualTo("%kim%");
		assertThat(UserSearchKeyword.of("10%").likePattern()).isEqualTo("%10\\%%");
		assertThat(UserSearchKeyword.of("a_b").likePattern()).isEqualTo("%a\\_b%");
		assertThat(UserSearchKeyword.of("a\\b").likePattern()).isEqualTo("%a\\\\b%");
		assertThat(UserSearchKeyword.of("%%").likePattern()).isEqualTo("%\\%\\%%");
	}

	@Test
	@DisplayName("toString에 검색어 값을 넣지 않는다(로그 유출 방지)")
	void toStringHidesValue() {
		assertThat(UserSearchKeyword.of("비밀검색어").toString()).doesNotContain("비밀검색어").contains("length=5");
	}

	private static void expectInvalid(final String raw) {
		assertThatThrownBy(() -> UserSearchKeyword.of(raw))
			.isInstanceOf(CustomException.class)
			.extracting(ex -> ((CustomException)ex).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_USER_SEARCH_KEYWORD);
	}
}
