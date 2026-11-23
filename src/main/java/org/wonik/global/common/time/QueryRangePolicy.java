package org.wonik.global.common.time;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * 기간 조회 범위 규칙. 기간을 제한해 목록 응답 크기를 묶는다(개인 일정·업무일지 조회는 페이지 대신 기간으로 제한한다).
 */
public final class QueryRangePolicy {

	public static final int MAX_DAYS = 366;

	private QueryRangePolicy() { }

	/** 시각 기간 [from, to). 시작은 종료보다 앞이고 MAX_DAYS 이내여야 한다. */
	public static void requireInstantRange(final Instant from, final Instant to) {
		if (!from.isBefore(to) || Duration.between(from, to).compareTo(Duration.ofDays(MAX_DAYS)) > 0) {
			throw new CustomException(ErrorCode.INVALID_QUERY_RANGE);
		}
	}

	/** 날짜 기간 [from, to], 양끝 날짜 포함. 시작은 종료보다 늦을 수 없고 MAX_DAYS 이내여야 한다. */
	public static void requireDateRange(final LocalDate from, final LocalDate to) {
		if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
			throw new CustomException(ErrorCode.INVALID_QUERY_RANGE);
		}
	}
}
