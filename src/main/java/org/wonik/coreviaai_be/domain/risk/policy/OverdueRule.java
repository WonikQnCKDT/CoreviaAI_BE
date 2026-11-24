package org.wonik.coreviaai_be.domain.risk.policy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.wonik.coreviaai_be.domain.risk.entity.type.RiskReasonCode;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskTargetType;
import org.wonik.coreviaai_be.domain.risk.service.result.DetectedRisk;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

/**
 * 마감 경과(FR-AIR-01 OVERDUE). 미완료이고 종료일이 오늘보다 이전이면 지연일이다.
 * 종료일 당일은 아직 마감 안 지난 것으로 본다.
 */
public final class OverdueRule {

	private OverdueRule() {
	}

	public static Optional<DetectedRisk> evaluate(
		final Long projectId,
		final Long taskId,
		final WbsTaskStatus status,
		final LocalDate endDate,
		final LocalDate today
	) {
		if (status == WbsTaskStatus.DONE || !endDate.isBefore(today)) {
			return Optional.empty();
		}
		final long delayDays = ChronoUnit.DAYS.between(endDate, today);
		return Optional.of(new DetectedRisk(
			projectId,
			RiskReasonCode.OVERDUE,
			RiskTargetType.TASK,
			taskId,
			BigDecimal.valueOf(delayDays),
			null));
	}
}
