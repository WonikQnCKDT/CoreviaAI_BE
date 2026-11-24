package org.wonik.coreviaai_be.domain.risk.policy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Optional;

import org.wonik.coreviaai_be.domain.risk.entity.type.RiskReasonCode;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskTargetType;
import org.wonik.coreviaai_be.domain.risk.service.result.DetectedRisk;
import org.wonik.coreviaai_be.domain.wbs.entity.MilestoneDeadline;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;
import org.wonik.coreviaai_be.domain.wbs.service.result.MilestoneLinkedTask;

/**
 * 마일스톤 지연 예상(FR-AIR-01 MILESTONE_AT_RISK).
 * <ul>
 *   <li>이미 지연: {@link MilestoneDeadline}과 같음(목표일 당일부터 미완료 연결 작업)</li>
 *   <li>예상: 목표일 전인데 미완료 연결 작업의 end_date가 목표일을 넘김</li>
 * </ul>
 * 연결 작업이 없으면 판단 근거가 없어 감지하지 않는다.
 */
public final class MilestoneAtRiskRule {

	private MilestoneAtRiskRule() {
	}

	public static Optional<DetectedRisk> evaluate(
		final Long projectId,
		final Long milestoneId,
		final LocalDate targetDate,
		final LocalDate today,
		final Collection<MilestoneLinkedTask> linkedTasks
	) {
		if (linkedTasks.isEmpty()) {
			return Optional.empty();
		}
		final Collection<WbsTaskStatus> statuses = linkedTasks.stream()
			.map(MilestoneLinkedTask::status)
			.toList();
		final MilestoneDeadline deadline = MilestoneDeadline.evaluate(targetDate, today, statuses);
		if (deadline.delayed()) {
			final long overdueDays = Math.max(0, ChronoUnit.DAYS.between(targetDate, today));
			return Optional.of(new DetectedRisk(
				projectId,
				RiskReasonCode.MILESTONE_AT_RISK,
				RiskTargetType.MILESTONE,
				milestoneId,
				BigDecimal.valueOf(overdueDays),
				null));
		}
		long maxOverrun = 0;
		boolean predicted = false;
		for (final MilestoneLinkedTask linked : linkedTasks) {
			if (linked.status() == WbsTaskStatus.DONE) {
				continue;
			}
			if (linked.endDate().isAfter(targetDate)) {
				predicted = true;
				maxOverrun = Math.max(maxOverrun, ChronoUnit.DAYS.between(targetDate, linked.endDate()));
			}
		}
		if (!predicted) {
			return Optional.empty();
		}
		return Optional.of(new DetectedRisk(
			projectId,
			RiskReasonCode.MILESTONE_AT_RISK,
			RiskTargetType.MILESTONE,
			milestoneId,
			BigDecimal.valueOf(maxOverrun),
			null));
	}
}
