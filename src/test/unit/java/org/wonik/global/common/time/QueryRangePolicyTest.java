package org.wonik.global.common.time;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

class QueryRangePolicyTest {

	private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");

	@Test
	void instantRangeAcceptsExactlyMaxDays() {
		assertThatCode(() -> QueryRangePolicy.requireInstantRange(START, START.plusSeconds(366L * 86_400)))
			.doesNotThrowAnyException();
	}

	@Test
	void instantRangeRejectsOneSecondOverMaxDays() {
		assertThatThrownBy(() -> QueryRangePolicy.requireInstantRange(START, START.plusSeconds(366L * 86_400 + 1)))
			.isInstanceOfSatisfying(CustomException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_QUERY_RANGE));
	}

	@Test
	void instantRangeRejectsEmptyOrReversedRange() {
		assertThatThrownBy(() -> QueryRangePolicy.requireInstantRange(START, START))
			.isInstanceOf(CustomException.class);
		assertThatThrownBy(() -> QueryRangePolicy.requireInstantRange(START.plusSeconds(1), START))
			.isInstanceOf(CustomException.class);
	}

	@Test
	void dateRangeIncludesBothEndsAndAcceptsSingleDay() {
		final LocalDate day = LocalDate.of(2026, 11, 9);
		assertThatCode(() -> QueryRangePolicy.requireDateRange(day, day)).doesNotThrowAnyException();
	}

	@Test
	void dateRangeAcceptsExactlyMaxDaysInclusive() {
		final LocalDate from = LocalDate.of(2026, 1, 1);
		assertThatCode(() -> QueryRangePolicy.requireDateRange(from, from.plusDays(QueryRangePolicy.MAX_DAYS - 1)))
			.doesNotThrowAnyException();
	}

	@Test
	void dateRangeRejectsOneDayOverMaxAndReversedRange() {
		final LocalDate from = LocalDate.of(2026, 1, 1);
		assertThatThrownBy(() -> QueryRangePolicy.requireDateRange(from, from.plusDays(QueryRangePolicy.MAX_DAYS)))
			.isInstanceOf(CustomException.class);
		assertThatThrownBy(() -> QueryRangePolicy.requireDateRange(from, from.minusDays(1)))
			.isInstanceOf(CustomException.class);
	}
}
