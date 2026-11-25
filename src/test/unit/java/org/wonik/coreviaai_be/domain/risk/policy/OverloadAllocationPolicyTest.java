package org.wonik.coreviaai_be.domain.risk.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.risk.policy.OverloadAllocationPolicy.Hit;
import org.wonik.coreviaai_be.domain.risk.policy.OverloadAllocationPolicy.TaskLoad;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

class OverloadAllocationPolicyTest {

	private static final int THRESHOLD = 120;
	/** 2026-11-02(월) ~ 2026-11-13(금) = 주말 2일 제외 10근무일. */
	private static final LocalDate START = LocalDate.of(2026, 11, 2);
	private static final LocalDate END = LocalDate.of(2026, 11, 13);

	@Test
	@DisplayName("할당/가용이 임계값과 같으면 편중이 아니고, 넘으면 편중이다")
	void thresholdIsExclusive() {
		assertThat(evaluate(load(1L, null, 7L, new BigDecimal("12.0")))).isEmpty();
		final List<Hit> over = evaluate(load(1L, null, 7L, new BigDecimal("12.1")));
		assertThat(over).hasSize(1);
		assertThat(over.get(0).allocationPercent()).isEqualByComparingTo("121.00");
		assertThat(over.get(0).availableDays()).isEqualTo(10);
		assertThat(over.get(0).allocatedEffort()).isEqualByComparingTo("12.1");
	}

	@Test
	@DisplayName("완료·공수 없음·담당자 없음·상위 작업 공수는 합계에서 빠진다")
	void ignoresDoneUnassignedParentAndMissingEffort() {
		final TaskLoad parent = load(10L, null, 1L, new BigDecimal("100.0"));
		final TaskLoad child = new TaskLoad(1L, 11L, 10L, WbsTaskStatus.TODO, new BigDecimal("12.1"), START, END);
		final TaskLoad done = new TaskLoad(1L, 12L, null, WbsTaskStatus.DONE, new BigDecimal("50.0"), START, END);
		final TaskLoad unassigned = new TaskLoad(null, 13L, null, WbsTaskStatus.TODO, new BigDecimal("50.0"), START, END);
		final TaskLoad noEffort = new TaskLoad(1L, 14L, null, WbsTaskStatus.TODO, null, START, END);
		final TaskLoad zero = new TaskLoad(2L, 15L, null, WbsTaskStatus.IN_PROGRESS, BigDecimal.ZERO, START, END);

		final List<Hit> hits = OverloadAllocationPolicy.evaluate(
			List.of(parent, child, done, unassigned, noEffort, zero), THRESHOLD);
		assertThat(hits).extracting(Hit::userId).containsExactly(1L);
		assertThat(hits.get(0).allocatedEffort()).isEqualByComparingTo("12.1");
	}

	@Test
	@DisplayName("근무일이 0이고 할당 공수가 있으면 어떤 임계값도 넘는 편중이다")
	void zeroAvailableDaysIsOverload() {
		final LocalDate saturday = LocalDate.of(2026, 11, 21);
		final LocalDate sunday = LocalDate.of(2026, 11, 22);
		final TaskLoad weekend = new TaskLoad(3L, 1L, null, WbsTaskStatus.TODO, new BigDecimal("1.0"), saturday, sunday);
		final List<Hit> hits = evaluate(weekend);
		assertThat(hits).hasSize(1);
		assertThat(hits.get(0).availableDays()).isZero();
		assertThat(hits.get(0).allocationPercent()).isNull();
	}

	@Test
	@DisplayName("기간은 담당 작업의 가장 이른 시작일과 가장 늦은 종료일이다")
	void periodSpansAssignedTasks() {
		final TaskLoad early = new TaskLoad(4L, 1L, null, WbsTaskStatus.TODO, new BigDecimal("1.0"),
			LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 2));
		final TaskLoad late = new TaskLoad(4L, 2L, null, WbsTaskStatus.REVIEW, new BigDecimal("20.0"),
			LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 13));
		final List<Hit> hits = OverloadAllocationPolicy.evaluate(List.of(early, late), THRESHOLD);
		assertThat(hits).hasSize(1);
		assertThat(hits.get(0).availableDays()).isEqualTo(10);
		assertThat(hits.get(0).allocatedEffort()).isEqualByComparingTo("21.0");
	}

	@Test
	@DisplayName("빈 목록·음수 임계값은 감지하지 않는다")
	void emptyAndNegativeThreshold() {
		assertThat(OverloadAllocationPolicy.evaluate(List.of(), THRESHOLD)).isEmpty();
		assertThat(OverloadAllocationPolicy.evaluate(List.of(load(1L, null, 1L, new BigDecimal("99"))), -1)).isEmpty();
		assertThat(OverloadAllocationPolicy.weekdaysInclusive(END, START)).isZero();
	}

	private static List<Hit> evaluate(final TaskLoad... loads) {
		return OverloadAllocationPolicy.evaluate(List.of(loads), THRESHOLD);
	}

	private static TaskLoad load(final Long taskId, final Long parentId, final Long assigneeId, final BigDecimal effort) {
		return new TaskLoad(assigneeId, taskId, parentId, WbsTaskStatus.TODO, effort, START, END);
	}
}
