package org.wonik.global.jwt;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 토큰 쿠키 발급·삭제·읽기.
 * <ul>
 *   <li>HttpOnly: 스크립트에서 토큰을 읽지 못하게 한다(XSS 탈취 방지).</li>
 *   <li>SameSite=Lax: 다른 사이트에서 보낸 POST·PATCH에 쿠키가 붙지 않아 CSRF를 막는다.
 *       FE와 BE는 nginx로 같은 origin에서 서비스한다는 전제다.</li>
 *   <li>Max-Age 없음(브라우저 세션 쿠키): 브라우저를 닫으면 사라진다. AccessToken이 만료돼도 쿠키는 남아
 *       서버가 EXPIRED_TOKEN으로 응답할 수 있고, FE는 그 코드로 refresh를 시도한다.</li>
 *   <li>RefreshToken은 /api/auth 경로에만 보낸다(refresh·logout 외 요청에 실리지 않게).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AuthCookieManager {

	public static final String ACCESS_TOKEN_COOKIE = "AccessToken";
	public static final String REFRESH_TOKEN_COOKIE = "RefreshToken";
	private static final String ACCESS_TOKEN_PATH = "/";
	private static final String REFRESH_TOKEN_PATH = "/api/auth";
	private static final String SAME_SITE = "Lax";

	private final JwtProperties jwtProperties;

	public void writeTokens(final HttpServletResponse response, final String accessToken, final String refreshToken) {
		response.addHeader(
				HttpHeaders.SET_COOKIE,
				cookie(ACCESS_TOKEN_COOKIE, accessToken, ACCESS_TOKEN_PATH, null)
		);
		response.addHeader(
				HttpHeaders.SET_COOKIE,
				cookie(REFRESH_TOKEN_COOKIE, refreshToken, REFRESH_TOKEN_PATH, null)
		);
	}

	public void writeAccessToken(final HttpServletResponse response, final String accessToken) {
		response.addHeader(
				HttpHeaders.SET_COOKIE,
				cookie(ACCESS_TOKEN_COOKIE, accessToken, ACCESS_TOKEN_PATH, null)
		);
	}

	public void expireTokens(final HttpServletResponse response) {
		response.addHeader(
				HttpHeaders.SET_COOKIE,
				cookie(ACCESS_TOKEN_COOKIE, "", ACCESS_TOKEN_PATH, Duration.ZERO)
		);
		response.addHeader(
				HttpHeaders.SET_COOKIE,
				cookie(REFRESH_TOKEN_COOKIE, "", REFRESH_TOKEN_PATH, Duration.ZERO)
		);
	}

	public Optional<String> readAccessToken(final HttpServletRequest request) {
		return read(request, ACCESS_TOKEN_COOKIE);
	}

	public Optional<String> readRefreshToken(final HttpServletRequest request) {
		return read(request, REFRESH_TOKEN_COOKIE);
	}

	private String cookie(final String name, final String value, final String path, final Duration maxAge) {
		final ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
			.httpOnly(true)
			.secure(jwtProperties.cookieSecure())
			.sameSite(SAME_SITE)
			.path(path);
		if (maxAge != null) {
			builder.maxAge(maxAge);
		}
		return builder.build().toString();
	}

	private static Optional<String> read(final HttpServletRequest request, final String name) {
		final Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		return Arrays.stream(cookies)
			.filter(cookie -> name.equals(cookie.getName()))
			.map(Cookie::getValue)
			.filter(value -> !value.isBlank())
			.findFirst();
	}
}
