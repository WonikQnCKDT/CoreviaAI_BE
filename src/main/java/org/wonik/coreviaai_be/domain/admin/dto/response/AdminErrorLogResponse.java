package org.wonik.coreviaai_be.domain.admin.dto.response;

import java.time.Instant;

import org.wonik.global.errorlog.ErrorLog;

/** 에러 로그 목록 항목. 스택 트레이스는 상세 조회에서만 준다(목록 응답 크기 제한). 메시지는 저장 시 마스킹된 값이다. */
public record AdminErrorLogResponse(
	Long id,
	Instant occurredAt,
	String httpMethod,
	String apiPath,
	Long userId,
	String errorType,
	String message
) {

	public static AdminErrorLogResponse from(final ErrorLog errorLog) {
		return new AdminErrorLogResponse(
			errorLog.getId(),
			errorLog.getOccurredAt(),
			errorLog.getHttpMethod(),
			errorLog.getApiPath(),
			errorLog.getUserId(),
			errorLog.getErrorType(),
			errorLog.getMessage()
		);
	}
}
