package org.wonik.coreviaai_be.domain.wbs.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.DONE;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.IN_PROGRESS;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.REVIEW;
import static org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.TODO;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

class MilestoneDeadlineTest {

	private static final LocalDate TARGET = LocalDate.of(2026, 12, 20);

	@ParameterizedTest(name = "오늘 {0} → D-day {1}")
	@CsvSource({
		"2026-12-19, 1",
		"2026-12-20, 0",
		"2026-12-21, -1",
		"2026-11-20, 30",
		"2027-01-01, -12"
	})
	@DisplayName("D-day는 오늘부터 목표일까지 일수다(내일 1, 당일 0, 어제 -1)")
	void dDay(final LocalDate today, final long expected) {
		assertThat(MilestoneDeadline.evaluate(TARGET, today, List.of()).dDay()).isEqualTo(expected);
	}

	@ParameterizedTest(name = "오늘 {0}, 미완료 작업 있음 → 경고 {1}")
	@CsvSource({
		"2026-12-19, false",
		"2026-12-20, true",
		"2026-12-21, true"
	})
	@DisplayName("미완료 연결 작업이 있으면 목표일 당일부터 지연 경고, 전날까지는 경고하지 않는다")
	void delayedFromTargetDate(final LocalDate today, final boolean expected) {
		assertThat(MilestoneDeadline.evaluate(TARGET, today, List.of(DONE, TODO)).delayed()).isEqualTo(expected);
	}

	@Test
	@DisplayName("목표일이 지나도 연결 작업이 모두 완료면 경고하지 않는다")
	void allDone() {
		final MilestoneDeadline deadline = MilestoneDeadline.evaluate(TARGET, TARGET.plusDays(5), List.of(DONE, DONE));
		assertThat(deadline.delayed()).isFalse();
		assertThat(deadline.linkedTaskCount()).isEqualTo(2);
		assertThat(deadline.incompleteTaskCount()).isZero();
	}

	@Test
	@DisplayName("연결 작업이 없으면 목표일이 지나도 경고하지 않는다(판단 근거 없음)")
	void noLinkedTasks() {
		final MilestoneDeadline deadline = MilestoneDeadline.evaluate(TARGET, TARGET.plusDays(5), List.of());
		assertThat(deadline.delayed()).isFalse();
		assertThat(deadline.linkedTaskCount()).isZero();
		assertThat(deadline.incompleteTaskCount()).isZero();
	}

	@Test
	@DisplayName("DONE이 아닌 상태(TODO·IN_PROGRESS·REVIEW)는 모두 미완료로 센다. 경고 전에도 미완료 수는 계산한다")
	void incompleteStatuses() {
		final List<WbsTaskStatus> statuses = List.of(TODO, IN_PROGRESS, REVIEW, DONE);
		final MilestoneDeadline before = MilestoneDeadline.evaluate(TARGET, TARGET.minusDays(1), statuses);
		assertThat(before.delayed()).isFalse();
		assertThat(before.incompleteTaskCount()).isEqualTo(3);
		assertThat(before.linkedTaskCount()).isEqualTo(4);

		assertThat(MilestoneDeadline.evaluate(TARGET, TARGET, List.of(REVIEW)).delayed()).isTrue();
		assertThat(MilestoneDeadline.evaluate(TARGET, TARGET, List.of(IN_PROGRESS)).incompleteTaskCount()).isEqualTo(1);
	}

	@ParameterizedTest(name = "오늘 {0} → 부모 TODO·하위 전부 DONE이면 경고 없음")
	@CsvSource({
		"2026-12-19",
		"2026-12-20",
		"2026-12-21"
	})
	@DisplayName("부모 저장 상태가 미완료여도 하위작업이 모두 DONE이면 경계일(D-1·D·D+1) 모두 경고하지 않는다")
	void derivedDoneParentNotDelayed(final LocalDate today) {
		final WbsTaskStatus effective = WbsTaskStatus.deriveFromSubtasks(TODO, List.of(DONE, DONE));
		final MilestoneDeadline deadline = MilestoneDeadline.evaluate(TARGET, today, List.of(effective));
		assertThat(deadline.delayed()).isFalse();
		assertThat(deadline.incompleteTaskCount()).isZero();
	}

	@ParameterizedTest(name = "오늘 {0} → 부모 DONE·하위 일부 미완료면 경고 {1}")
	@CsvSource({
		"2026-12-19, false",
		"2026-12-20, true",
		"2026-12-21, true"
	})
	@DisplayName("부모 저장 상태가 DONE이어도 하위작업 일부가 미완료면 당일(D)·다음날(D+1) 경고하고 전날(D-1)은 경고하지 않는다")
	void derivedIncompleteParentDelayedOnBoundary(final LocalDate today, final boolean expected) {
		final WbsTaskStatus effective = WbsTaskStatus.deriveFromSubtasks(DONE, List.of(DONE, TODO));
		final MilestoneDeadline deadline = MilestoneDeadline.evaluate(TARGET, today, List.of(effective));
		assertThat(deadline.delayed()).isEqualTo(expected);
		assertThat(deadline.incompleteTaskCount()).isEqualTo(1);
	}
}
