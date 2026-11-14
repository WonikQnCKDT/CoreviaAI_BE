package org.wonik.authserver.domain.auth.service;

import org.wonik.authserver.domain.auth.authenticator.AuthenticationOutcome;

/**
 * 로그인 성공 결과. 토큰은 컨트롤러가 쿠키로만 내려보내고 응답 본문에는 넣지 않는다.
 */
public record IssuedLogin(String accessToken, String refreshToken, AuthenticationOutcome account) {
}
