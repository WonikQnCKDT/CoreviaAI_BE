package org.wonik.coreviaai_be.domain.risk.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.DONE;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.IN_PROGRESS;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.TODO;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskReasonCode;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskTargetType;

class OverdueRuleTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

	@ParameterizedTest(name = "종료 {0}, 상태 {1} → 감지 {2}")
	@CsvSource({
		"2026-11-23, IN_PROGRESS, true",
		"2026-11-24, IN_PROGRESS, false",
		"2026-11-25, TODO, false",
		"2026-11-20, DONE, false"
	})
	@DisplayName("미완료이고 종료일이 오늘보다 이전일 때만 OVERDUE")
	void evaluates(final LocalDate end, final String statusName, final boolean expected) {
		final var status = switch (statusName) {
			case "DONE" -> DONE;
			case "TODO" -> TODO;
			default -> IN_PROGRESS;
		};
		assertThat(OverdueRule.evaluate(1L, 10L, status, end, TODAY).isPresent()).isEqualTo(expected);
	}

	@Test
	@DisplayName("지연일은 종료일부터 오늘까지 일수다")
	void delayDays() {
		final var detected = OverdueRule.evaluate(1L, 10L, IN_PROGRESS, LocalDate.of(2026, 11, 20), TODAY).orElseThrow();
		assertThat(detected.reasonCode()).isEqualTo(RiskReasonCode.OVERDUE);
		assertThat(detected.targetType()).isEqualTo(RiskTargetType.TASK);
		assertThat(detected.metricValue()).isEqualByComparingTo(BigDecimal.valueOf(4));
	}
}
