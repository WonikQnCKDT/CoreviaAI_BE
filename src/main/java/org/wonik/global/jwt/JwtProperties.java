package org.wonik.global.jwt;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT·토큰 쿠키 설정(app.jwt.*). 값과 근거는 CoreviaAI_BE/README.md "인증" 절에 기록한다.
 *
 * @param secret          서명 키(Base64, 32바이트 이상). 환경변수 JWT_SECRET으로만 주입한다.
 * @param accessTokenTtl  AccessToken 수명. 만료되면 FE가 refresh로 재발급받는다.
 * @param refreshTokenTtl RefreshToken 수명이자 한 번 로그인으로 유지할 수 있는 최대 시간
 * @param cookieSecure    HTTPS에서만 쿠키 전송. 로컬 HTTP 테스트에서만 false
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
	String secret,
	Duration accessTokenTtl,
	Duration refreshTokenTtl,
	boolean cookieSecure
) {
}
