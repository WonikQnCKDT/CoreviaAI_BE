package org.wonik.global.errorlog;

import java.time.Instant;

import org.wonik.global.common.masking.SensitiveValueMasker;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 에러 로그(FR-ADM-09). 전역 예외 처리기에 도달한 미처리 예외를 요청 정보와 함께 남기는 append-only 기록이다.
 * <p>
 * 메시지·스택 트레이스는 민감값을 가린 뒤 길이를 자른다(가린 다음 자른다: 먼저 자르면 잘린 토큰 조각이 패턴에 걸리지 않고 남는다).
 * 요청 본문·헤더·쿼리 문자열은 받지 않는다. 사용자는 ID 값만 남긴다(FK 없음, 로그 보존).
 * authserver(로그인)와 업무 모듈 요청이 모두 같은 처리기를 지나므로 global에 둔다.
 */
@Getter
@Entity
@Table(name = "error_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorLog {

	static final int HTTP_METHOD_MAX_LENGTH = 10;
	static final int API_PATH_MAX_LENGTH = 300;
	static final int ERROR_TYPE_MAX_LENGTH = 200;
	static final int MESSAGE_MAX_LENGTH = 2000;
	/** stack_trace 컬럼은 TEXT지만 예외 폭주 시 저장량을 묶기 위해 서비스에서 자른다. */
	static final int STACK_TRACE_MAX_LENGTH = 50_000;
	/** 마스킹 전에 원문을 이 길이로 먼저 줄인다(마스킹 시간 상한). 저장 길이보다 충분히 길어 경계에서 잘린 값이 저장되지 않는다. */
	static final int RAW_INPUT_MAX_LENGTH = 100_000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	@Column(name = "http_method", updatable = false, length = HTTP_METHOD_MAX_LENGTH)
	private String httpMethod;

	@Column(name = "api_path", updatable = false, length = API_PATH_MAX_LENGTH)
	private String apiPath;

	@Column(name = "user_id", updatable = false)
	private Long userId;

	@Column(name = "error_type", nullable = false, updatable = false, length = ERROR_TYPE_MAX_LENGTH)
	private String errorType;

	@Column(name = "message", updatable = false, length = MESSAGE_MAX_LENGTH)
	private String message;

	/** DB 타입은 TEXT. PostgreSQL·H2 모두 TEXT를 VARCHAR 계열로 알려 주므로 String 기본 매핑으로 둔다(ai_drafts.output과 같음). */
	@Column(name = "stack_trace", updatable = false)
	private String stackTrace;

	private ErrorLog(
		final Instant occurredAt,
		final String httpMethod,
		final String apiPath,
		final Long userId,
		final String errorType,
		final String message,
		final String stackTrace
	) {
		this.occurredAt = occurredAt;
		this.httpMethod = httpMethod;
		this.apiPath = apiPath;
		this.userId = userId;
		this.errorType = errorType;
		this.message = message;
		this.stackTrace = stackTrace;
	}

	/**
	 * @param httpMethod 요청 메서드. 요청 밖에서 생긴 오류면 null
	 * @param apiPath    쿼리 문자열을 뺀 요청 경로. 요청 밖이면 null
	 * @param userId     로그인 사용자 ID. 로그인 전 요청이면 null
	 */
	public static ErrorLog of(
		final Instant occurredAt,
		final String httpMethod,
		final String apiPath,
		final Long userId,
		final Throwable error
	) {
		return new ErrorLog(
			occurredAt,
			truncate(httpMethod, HTTP_METHOD_MAX_LENGTH),
			maskAndTruncate(apiPath, API_PATH_MAX_LENGTH),
			userId,
			truncate(error.getClass().getName(), ERROR_TYPE_MAX_LENGTH),
			maskAndTruncate(error.getMessage(), MESSAGE_MAX_LENGTH),
			truncate(StackTraceFormatter.format(error), STACK_TRACE_MAX_LENGTH)
		);
	}

	static String maskAndTruncate(final String raw, final int maxLength) {
		return truncate(SensitiveValueMasker.mask(truncate(raw, RAW_INPUT_MAX_LENGTH)), maxLength);
	}

	static String truncate(final String value, final int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength);
	}
}
