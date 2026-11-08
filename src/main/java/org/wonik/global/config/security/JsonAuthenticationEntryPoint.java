package org.wonik.global.config.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.wonik.global.common.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 미인증 요청을 401 JSON으로 응답한다.
 * <p>
 * 인증 filter가 실패 사유(토큰 만료, 세션 만료 등)를 {@link #ERROR_CODE_ATTRIBUTE}에 남기면 그 코드로 응답한다.
 * FE는 EXPIRED_TOKEN이면 refresh를 시도하고, 그 외 401이면 로그인 화면으로 이동한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

	public static final String ERROR_CODE_ATTRIBUTE = JsonAuthenticationEntryPoint.class.getName() + ".errorCode";

	private final SecurityErrorResponseWriter errorResponseWriter;

	@Override
	public void commence(
		final HttpServletRequest request,
		final HttpServletResponse response,
		final AuthenticationException authException
	) throws IOException {
		final Object attribute = request.getAttribute(ERROR_CODE_ATTRIBUTE);
		final ErrorCode errorCode = attribute instanceof ErrorCode code
				? code
				: ErrorCode.UNAUTHORIZED;
		log.debug("인증되지 않은 요청 - uri={}, code={}", request.getRequestURI(), errorCode.name());
		errorResponseWriter.write(response, errorCode);
	}
}
