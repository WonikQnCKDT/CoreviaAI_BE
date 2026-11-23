package org.wonik.coreviaai_be.domain.wbs.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;

/** 예상 공수 가중 평균 진척률. 공수가 없는 작업도 빠지지 않도록 1인일 가중치로 계산한다. */
public final class ProgressCalculator {

	private ProgressCalculator() {
	}

	public static int weightedAverage(final Collection<WbsTask> tasks) {
		if (tasks.isEmpty()) {
			return 0;
		}
		BigDecimal weightedProgress = BigDecimal.ZERO;
		BigDecimal totalWeight = BigDecimal.ZERO;
		for (final WbsTask task : tasks) {
			final BigDecimal weight = task.getEstimatedEffort() == null ? BigDecimal.ONE : task.getEstimatedEffort();
			weightedProgress = weightedProgress.add(weight.multiply(BigDecimal.valueOf(task.getProgress())));
			totalWeight = totalWeight.add(weight);
		}
		return weightedProgress.divide(totalWeight, 0, RoundingMode.HALF_UP).intValueExact();
	}
}
