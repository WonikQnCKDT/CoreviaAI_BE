package org.wonik.coreviaai_be.domain.admin.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.errorlog.ErrorLog;
import org.wonik.global.errorlog.ErrorLogRepository;

import lombok.RequiredArgsConstructor;

/**
 * 시스템 관리자의 에러 로그 조회(FR-ADM-09). 조회만 한다(권한 3.3: 시스템 관리자 R, 수정·삭제 없음).
 * 기록은 전역 예외 처리기가 ErrorLogRecorder로 한다.
 */
@Service
@RequiredArgsConstructor
public class AdminErrorLogService {

	/** 기간 조건이 없을 때 쓰는 경계. 쿼리에 NULL 시각을 넘기지 않으려고 둔다(DB 타입 추론 문제). */
	static final Instant NO_LOWER_BOUND = Instant.parse("1970-01-01T00:00:00Z");
	static final Instant NO_UPPER_BOUND = Instant.parse("9999-12-31T00:00:00Z");
	private static final String LIKE_ESCAPE = "\\";

	private final ErrorLogRepository errorLogRepository;

	/**
	 * @param from      발생 시각 하한(포함). 없으면 제한 없음
	 * @param to        발생 시각 상한(제외). 없으면 제한 없음. from보다 늦어야 한다
	 * @param errorType 예외 클래스 이름(정확히 일치, 예: java.lang.IllegalStateException). 없거나 비면 전체
	 * @param apiPath   요청 경로 앞부분 일치(대소문자 구분, 예: /api/projects). 없거나 비면 전체
	 */
	@Transactional(readOnly = true)
	public CursorPage<ErrorLog> findPage(
		final CursorPageRequest page,
		final Instant from,
		final Instant to,
		final String errorType,
		final String apiPath
	) {
		if (from != null && to != null && !from.isBefore(to)) {
			throw new CustomException(ErrorCode.INVALID_PERIOD);
		}
		final List<ErrorLog> fetched = errorLogRepository.findPageForAdmin(
			page.cursorId(),
			from == null ? NO_LOWER_BOUND : from,
			to == null ? NO_UPPER_BOUND : to,
			errorType == null ? "" : errorType.strip(),
			prefixPattern(apiPath),
			page.fetchPageable()
		);
		return CursorPage.of(fetched, page.size(), ErrorLog::getId);
	}

	@Transactional(readOnly = true)
	public ErrorLog find(final Long errorLogId) {
		return errorLogRepository.findById(errorLogId)
			.orElseThrow(() -> new CustomException(ErrorCode.ERROR_LOG_NOT_FOUND));
	}

	/** 앞부분 일치 LIKE 패턴. 와일드카드(%, _)와 이스케이프 문자는 글자 그대로 찾도록 이스케이프한다. 없으면 '%'(전체). */
	static String prefixPattern(final String apiPath) {
		if (apiPath == null || apiPath.isBlank()) {
			return "%";
		}
		final String escaped = apiPath.strip()
			.replace(LIKE_ESCAPE, LIKE_ESCAPE + LIKE_ESCAPE)
			.replace("%", LIKE_ESCAPE + "%")
			.replace("_", LIKE_ESCAPE + "_");
		return escaped + "%";
	}
}
