package org.wonik.global.security;

import org.springframework.http.HttpMethod;

/**
 * 메서드와 경로가 정확히 같은 요청 하나를 가리킨다.
 */
public record AllowedRequest(HttpMethod method, String path) {

	public boolean matches(final String requestMethod, final String requestPath) {
		return method.matches(requestMethod) && path.equals(requestPath);
	}
}
