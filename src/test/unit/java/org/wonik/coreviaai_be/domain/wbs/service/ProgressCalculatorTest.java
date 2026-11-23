package org.wonik.coreviaai_be.domain.wbs.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskDetails;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;

class ProgressCalculatorTest {

	@Test
	@DisplayName("예상 공수로 가중 평균하고 .5는 반올림하며 공수가 없으면 1인일로 계산한다")
	void weightedAverage() {
		final WbsTask oneDay = task(new BigDecimal("1.0"), 100);
		final WbsTask threeDays = task(new BigDecimal("3.0"), 50);
		assertThat(ProgressCalculator.weightedAverage(List.of(oneDay, threeDays))).isEqualTo(63);

		final WbsTask unspecified = task(null, 0);
		assertThat(ProgressCalculator.weightedAverage(List.of(oneDay, unspecified))).isEqualTo(50);
		assertThat(ProgressCalculator.weightedAverage(List.of())).isZero();
	}

	private static WbsTask task(final BigDecimal effort, final int progress) {
		final WbsTask task = WbsTask.task(1L, 1, new TaskDetails("작업", null, null,
			LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2), effort), WbsItemSource.MANUAL, 1L);
		task.changeProgress(progress);
		return task;
	}
}
