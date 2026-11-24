package org.wonik.coreviaai_be.domain.risk.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.DONE;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.IN_PROGRESS;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskReasonCode;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskTargetType;
import org.wonik.coreviaai_be.domain.wbs.service.result.MilestoneLinkedTask;

class MilestoneAtRiskRuleTest {

	private static final LocalDate TARGET = LocalDate.of(2026, 12, 1);

	@Test
	@DisplayName("목표일 도래·미완료 연결이면 지연(이미 지연)")
	void alreadyDelayed() {
		final var linked = List.of(linked(1L, IN_PROGRESS, TARGET.minusDays(2)));
		final var detected = MilestoneAtRiskRule.evaluate(
			1L, 5L, TARGET, TARGET, linked).orElseThrow();
		assertThat(detected.reasonCode()).isEqualTo(RiskReasonCode.MILESTONE_AT_RISK);
		assertThat(detected.targetType()).isEqualTo(RiskTargetType.MILESTONE);
		assertThat(detected.metricValue()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	@DisplayName("목표일 전인데 연결 작업 end_date가 목표를 넘으면 지연 예상")
	void predicted() {
		final var linked = List.of(linked(1L, IN_PROGRESS, TARGET.plusDays(3)));
		final var detected = MilestoneAtRiskRule.evaluate(
			1L, 5L, TARGET, TARGET.minusDays(5), linked).orElseThrow();
		assertThat(detected.metricValue()).isEqualByComparingTo(BigDecimal.valueOf(3));
	}

	@Test
	@DisplayName("연결 없음·모두 완료·기한 내 미완료면 감지하지 않는다")
	void safe() {
		assertThat(MilestoneAtRiskRule.evaluate(
			1L, 5L, TARGET, TARGET.plusDays(1), List.of())).isEmpty();
		assertThat(MilestoneAtRiskRule.evaluate(
			1L, 5L, TARGET, TARGET.plusDays(1),
			List.of(linked(1L, DONE, TARGET.plusDays(10))))).isEmpty();
		assertThat(MilestoneAtRiskRule.evaluate(
			1L, 5L, TARGET, TARGET.minusDays(1),
			List.of(linked(1L, IN_PROGRESS, TARGET)))).isEmpty();
	}

	private static MilestoneLinkedTask linked(
		final long taskId,
		final org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus status,
		final LocalDate endDate
	) {
		return new MilestoneLinkedTask(5L, taskId, null, "t", status, endDate);
	}
}
