package org.wonik.global.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.support.MutableClock;

class JwtTokenProviderTest {

	// 테스트 전용 키(운영 비밀값 아님)
	private static final String KEY_A = "Y29yZXZpYS10ZXN0LW9ubHktand0LXNpZ25pbmcta2V5LTAxMjM0NTY3ODk=";
	private static final String KEY_B = "YW5vdGhlci10ZXN0LW9ubHktand0LXNpZ25pbmcta2V5LTk4NzY1NDMyMTA=";
	private static final Instant START = Instant.parse("2026-11-09T00:00:00Z");

	private final MutableClock clock = new MutableClock(START);
	private final JwtTokenProvider provider = new JwtTokenProvider(properties(KEY_A), clock);

	private static JwtProperties properties(final String key) {
		return new JwtProperties(key, Duration.ofMinutes(10), Duration.ofHours(12), false);
	}

	@Test
	@DisplayName("발급한 AccessToken에서 사용자 ID와 세션 ID를 꺼낸다")
	void parsesIssuedAccessToken() {
		final TokenClaims claims = provider.parse(provider.createAccessToken(7L, "sid-1"), TokenType.ACCESS);

		assertThat(claims.userId()).isEqualTo(7L);
		assertThat(claims.sessionId()).isEqualTo("sid-1");
	}

	@Test
	@DisplayName("수명이 지난 AccessToken은 EXPIRED_TOKEN")
	void expiredAccessToken() {
		final String token = provider.createAccessToken(7L, "sid-1");
		clock.advance(Duration.ofMinutes(11));

		assertThatThrownBy(() -> provider.parse(token, TokenType.ACCESS))
			.isInstanceOfSatisfying(CustomException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.EXPIRED_TOKEN));
	}

	@Test
	@DisplayName("RefreshToken을 AccessToken 자리에 쓰면 INVALID_TOKEN")
	void rejectsWrongTokenType() {
		final String refreshToken = provider.createRefreshToken(7L, "sid-1");

		assertThatThrownBy(() -> provider.parse(refreshToken, TokenType.ACCESS))
			.isInstanceOfSatisfying(CustomException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
	}

	@Test
	@DisplayName("다른 키로 서명한 토큰은 INVALID_TOKEN")
	void rejectsForeignSignature() {
		final String foreign = new JwtTokenProvider(properties(KEY_B), clock).createAccessToken(7L, "sid-1");

		assertThatThrownBy(() -> provider.parse(foreign, TokenType.ACCESS))
			.isInstanceOfSatisfying(CustomException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
	}

	@Test
	@DisplayName("로그아웃용 세션 ID 추출은 만료된 토큰도 허용하지만 위조 토큰은 거부한다")
	void extractsSessionIdForLogout() {
		final String token = provider.createAccessToken(7L, "sid-1");
		clock.advance(Duration.ofHours(1));

		assertThat(provider.extractSessionIdAllowExpired(token)).contains("sid-1");
		assertThat(provider.extractSessionIdAllowExpired("not-a-jwt")).isEmpty();
	}

	@Test
	@DisplayName("32바이트보다 짧은 서명 키는 기동 시 거부한다")
	void rejectsShortKey() {
		assertThatThrownBy(() -> new JwtTokenProvider(properties("c2hvcnQta2V5"), clock))
			.isInstanceOf(IllegalStateException.class);
	}
}
