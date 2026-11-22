package org.wonik.coreviaai_be.domain.admin.dto.response;

import java.time.Instant;

import org.wonik.global.errorlog.ErrorLog;

/** 에러 로그 상세. 스택 트레이스(마스킹된 값)는 시스템 관리자 화면에서만 볼 수 있다. */
public record AdminErrorLogDetailResponse(
	Long id,
	Instant occurredAt,
	String httpMethod,
	String apiPath,
	Long userId,
	String errorType,
	String message,
	String stackTrace
) {

	public static AdminErrorLogDetailResponse from(final ErrorLog errorLog) {
		return new AdminErrorLogDetailResponse(
			errorLog.getId(),
			errorLog.getOccurredAt(),
			errorLog.getHttpMethod(),
			errorLog.getApiPath(),
			errorLog.getUserId(),
			errorLog.getErrorType(),
			errorLog.getMessage(),
			errorLog.getStackTrace()
		);
	}
}
