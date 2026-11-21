package org.wonik.global.common.time;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BusinessCalendarTest {

	@Test
	@DisplayName("한국 자정 경계: UTC 14:59:59는 그날, 15:00:00은 다음 날이다")
	void koreanMidnightBoundary() {
		assertThat(BusinessCalendar.today(Clock.fixed(Instant.parse("2026-12-19T14:59:59Z"), ZoneOffset.UTC)))
			.isEqualTo(LocalDate.of(2026, 12, 19));
		assertThat(BusinessCalendar.today(Clock.fixed(Instant.parse("2026-12-19T15:00:00Z"), ZoneOffset.UTC)))
			.isEqualTo(LocalDate.of(2026, 12, 20));
	}

	@Test
	@DisplayName("Clock의 시간대와 관계없이 같은 시점이면 같은 업무 날짜다")
	void ignoresClockZone() {
		final Instant instant = Instant.parse("2026-12-19T20:00:00Z");
		assertThat(BusinessCalendar.today(Clock.fixed(instant, ZoneId.of("America/Los_Angeles"))))
			.isEqualTo(BusinessCalendar.today(Clock.fixed(instant, ZoneOffset.UTC)))
			.isEqualTo(LocalDate.of(2026, 12, 20));
	}
}
