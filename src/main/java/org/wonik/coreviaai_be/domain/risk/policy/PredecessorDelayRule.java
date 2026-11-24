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
 * 선행 지연 전파(FR-AIR-01 PREDECESSOR_DELAY).
 * 선행이 미완료이고 종료일이 오늘보다 이전이면, 미완료 후행에 전파한다.
 * TaskDependencyService의 지연 판정과 같다(완료→시작).
 */
public final class PredecessorDelayRule {

	private PredecessorDelayRule() {
	}

	public static Optional<DetectedRisk> evaluate(
		final Long projectId,
		final Long successorTaskId,
		final WbsTaskStatus successorStatus,
		final Long predecessorTaskId,
		final WbsTaskStatus predecessorStatus,
		final LocalDate predecessorEndDate,
		final LocalDate today
	) {
		if (successorStatus == WbsTaskStatus.DONE) {
			return Optional.empty();
		}
		final boolean predecessorDelayed = predecessorStatus != WbsTaskStatus.DONE
			&& predecessorEndDate.isBefore(today);
		if (!predecessorDelayed) {
			return Optional.empty();
		}
		final long delayDays = ChronoUnit.DAYS.between(predecessorEndDate, today);
		final String details = "{\"predecessorTaskId\":" + predecessorTaskId
			+ ",\"predecessorDelayDays\":" + delayDays + "}";
		return Optional.of(new DetectedRisk(
			projectId,
			RiskReasonCode.PREDECESSOR_DELAY,
			RiskTargetType.TASK,
			successorTaskId,
			BigDecimal.valueOf(delayDays),
			details));
	}
}
