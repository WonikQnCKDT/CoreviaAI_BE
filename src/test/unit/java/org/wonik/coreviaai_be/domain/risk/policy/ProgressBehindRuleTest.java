package org.wonik.coreviaai_be.domain.risk.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.DONE;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.IN_PROGRESS;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ProgressBehindRuleTest {

	private static final LocalDate START = LocalDate.of(2026, 11, 1);
	private static final LocalDate END = LocalDate.of(2026, 11, 21);
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);

	@Test
	@DisplayName("기간 중간(10/20일)이면 경과율 50%")
	void elapsedMid() {
		assertThat(ProgressBehindRule.elapsedRatePercent(START, END, TODAY))
			.isEqualByComparingTo(new BigDecimal("50.00"));
	}

	@ParameterizedTest
	@CsvSource({
		"2026-11-01, 0.00",
		"2026-11-21, 100.00",
		"2026-11-30, 100.00"
	})
	@DisplayName("시작 당일 0%, 종료일 이후 100%")
	void elapsedBoundaries(final LocalDate today, final String expected) {
		assertThat(ProgressBehindRule.elapsedRatePercent(START, END, today))
			.isEqualByComparingTo(new BigDecimal(expected));
	}

	@Test
	@DisplayName("진척률이 경과율보다 낮으면 PROGRESS_BEHIND, metric은 차이")
	void behind() {
		final var detected = ProgressBehindRule.evaluate(
			1L, 10L, IN_PROGRESS, 20, START, END, TODAY, 0).orElseThrow();
		assertThat(detected.metricValue()).isEqualByComparingTo(new BigDecimal("30.00"));
	}

	@Test
	@DisplayName("마진 안이면 감지하지 않는다")
	void withinMargin() {
		assertThat(ProgressBehindRule.evaluate(
			1L, 10L, IN_PROGRESS, 45, START, END, TODAY, 10)).isEmpty();
	}

	@Test
	@DisplayName("완료·시작 전·경과율 이상 진척은 감지하지 않는다")
	void notBehind() {
		assertThat(ProgressBehindRule.evaluate(
			1L, 10L, DONE, 0, START, END, TODAY, 0)).isEmpty();
		assertThat(ProgressBehindRule.evaluate(
			1L, 10L, IN_PROGRESS, 0, START, END, START.minusDays(1), 0)).isEmpty();
		assertThat(ProgressBehindRule.evaluate(
			1L, 10L, IN_PROGRESS, 50, START, END, TODAY, 0)).isEmpty();
	}
}
