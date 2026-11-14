package org.wonik.authserver.domain.auth.controller;

import org.wonik.global.jwt.AuthCookieManager;

import jakarta.servlet.http.Cookie;

/**
 * 통합 테스트에서 로그인 응답 쿠키를 다시 보내기 위한 값 묶음.
 */
record Tokens(String access, String refresh) {

	Cookie accessCookie() {
		return new Cookie(AuthCookieManager.ACCESS_TOKEN_COOKIE, access);
	}

	Cookie refreshCookie() {
		return new Cookie(AuthCookieManager.REFRESH_TOKEN_COOKIE, refresh);
	}
}
