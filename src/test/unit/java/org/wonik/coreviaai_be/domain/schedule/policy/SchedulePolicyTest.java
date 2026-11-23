package org.wonik.coreviaai_be.domain.schedule.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.wonik.global.common.exception.CustomException;

class SchedulePolicyTest {

	private static final Instant START = Instant.parse("2026-11-09T01:00:00Z");

	@Test
	void acceptsEndAfterStart() {
		assertThatCode(() -> SchedulePolicy.validatePeriod(START, START.plusNanos(1))).doesNotThrowAnyException();
	}

	@Test
	void rejectsZeroLengthPeriod() {
		assertThatThrownBy(() -> SchedulePolicy.validatePeriod(START, START))
			.isInstanceOf(CustomException.class);
	}

	@Test
	void rejectsEndBeforeStart() {
		assertThatThrownBy(() -> SchedulePolicy.validatePeriod(START, START.minusSeconds(1)))
			.isInstanceOf(CustomException.class);
	}
}
