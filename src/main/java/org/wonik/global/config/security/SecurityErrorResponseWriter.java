package org.wonik.global.config.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.response.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * Security filter 단계에서 끝나는 오류(인증 실패, 권한 없음)는 GlobalExceptionHandler를 거치지 않는다.
 * 이 경우에도 API와 같은 ApiResponse 형식으로 응답하도록 한곳에서 직렬화한다.
 */
@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

	private final JsonMapper jsonMapper;

	public void write(final HttpServletResponse response, final ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.getStatus());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write(jsonMapper.writeValueAsString(ApiResponse.error(errorCode)));
	}
}
