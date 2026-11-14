package org.wonik.global.config.security;

import java.io.IOException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.wonik.global.common.exception.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증은 됐지만 권한이 없는 요청(예: 일반 사용자의 /api/admin/**)을 처리한다.
 * <p>
 * GlobalExceptionHandler의 AuthorizationDeniedException 처리와 같이 404로 응답해
 * 관리자 API의 존재를 일반 사용자에게 드러내지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

	private final SecurityErrorResponseWriter errorResponseWriter;

	@Override
	public void handle(
		final HttpServletRequest request,
		final HttpServletResponse response,
		final AccessDeniedException accessDeniedException
	) throws IOException {
		log.warn("권한 없는 요청 - uri={}", request.getRequestURI());
		errorResponseWriter.write(response, ErrorCode.NOT_FOUND);
	}
}
