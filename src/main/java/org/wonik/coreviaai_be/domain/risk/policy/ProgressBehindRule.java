package org.wonik.coreviaai_be.domain.risk.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.wonik.coreviaai_be.domain.risk.entity.type.RiskReasonCode;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskTargetType;
import org.wonik.coreviaai_be.domain.risk.service.result.DetectedRisk;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

/**
 * 진척률 &lt; 기간 경과율(FR-AIR-01 PROGRESS_BEHIND).
 * AI 진척률 추정(FR-AIR-14)은 쓰지 않고 저장된 progress만 본다.
 * 기간 경과율(%) = clamp((오늘−시작)/(종료−시작)×100, 0, 100). 시작=종료면 시작일 이후 100.
 * marginPercent 이상 뒤처질 때만 감지한다(기본 0 = 경과율보다 낮으면 즉시).
 */
public final class ProgressBehindRule {

	private ProgressBehindRule() {
	}

	public static Optional<DetectedRisk> evaluate(
		final Long projectId,
		final Long taskId,
		final WbsTaskStatus status,
		final int progress,
		final LocalDate startDate,
		final LocalDate endDate,
		final LocalDate today,
		final int marginPercent
	) {
		if (status == WbsTaskStatus.DONE || today.isBefore(startDate)) {
			return Optional.empty();
		}
		final BigDecimal elapsedRate = elapsedRatePercent(startDate, endDate, today);
		final BigDecimal threshold = elapsedRate.subtract(BigDecimal.valueOf(Math.max(0, marginPercent)));
		if (BigDecimal.valueOf(progress).compareTo(threshold) >= 0) {
			return Optional.empty();
		}
		final BigDecimal gap = elapsedRate.subtract(BigDecimal.valueOf(progress));
		return Optional.of(new DetectedRisk(
			projectId,
			RiskReasonCode.PROGRESS_BEHIND,
			RiskTargetType.TASK,
			taskId,
			gap,
			null));
	}

	/** 기간 경과율 0~100(%). 시작 전이면 호출하지 않는다. */
	public static BigDecimal elapsedRatePercent(
		final LocalDate startDate,
		final LocalDate endDate,
		final LocalDate today
	) {
		final long totalDays = ChronoUnit.DAYS.between(startDate, endDate);
		if (totalDays <= 0) {
			return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
		}
		final long elapsedDays = ChronoUnit.DAYS.between(startDate, today);
		if (elapsedDays <= 0) {
			return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
		}
		if (elapsedDays >= totalDays) {
			return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
		}
		return BigDecimal.valueOf(elapsedDays)
			.multiply(BigDecimal.valueOf(100))
			.divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
	}
}
