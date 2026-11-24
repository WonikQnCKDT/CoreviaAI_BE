package org.wonik.coreviaai_be.domain.risk.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.DONE;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.IN_PROGRESS;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.TODO;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskReasonCode;

class PredecessorDelayRuleTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

	@Test
	@DisplayName("선행이 마감 경과·미완료이면 미완료 후행에 PREDECESSOR_DELAY")
	void propagates() {
		final var detected = PredecessorDelayRule.evaluate(
			1L, 20L, TODO, 10L, IN_PROGRESS, LocalDate.of(2026, 11, 20), TODAY).orElseThrow();
		assertThat(detected.reasonCode()).isEqualTo(RiskReasonCode.PREDECESSOR_DELAY);
		assertThat(detected.targetId()).isEqualTo(20L);
		assertThat(detected.metricValue()).isEqualByComparingTo(BigDecimal.valueOf(4));
		assertThat(detected.detailsJson()).contains("\"predecessorTaskId\":10");
	}

	@Test
	@DisplayName("선행 완료·선행 미지연·후행 완료면 감지하지 않는다")
	void noPropagation() {
		assertThat(PredecessorDelayRule.evaluate(
			1L, 20L, TODO, 10L, DONE, LocalDate.of(2026, 11, 20), TODAY)).isEmpty();
		assertThat(PredecessorDelayRule.evaluate(
			1L, 20L, TODO, 10L, IN_PROGRESS, TODAY, TODAY)).isEmpty();
		assertThat(PredecessorDelayRule.evaluate(
			1L, 20L, DONE, 10L, IN_PROGRESS, LocalDate.of(2026, 11, 20), TODAY)).isEmpty();
	}
}
