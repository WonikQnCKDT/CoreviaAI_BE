package org.wonik.coreviaai_be.domain.schedule.policy;

import java.time.Instant;

import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

public final class SchedulePolicy {

	private SchedulePolicy() { }

	/** 일정은 시작 시각이 종료 시각보다 앞서야 한다. 같은 시각(길이 0)도 허용하지 않는다. */
	public static void validatePeriod(final Instant startAt, final Instant endAt) {
		if (!startAt.isBefore(endAt)) {
			throw new CustomException(ErrorCode.INVALID_SCHEDULE_PERIOD);
		}
	}
}
