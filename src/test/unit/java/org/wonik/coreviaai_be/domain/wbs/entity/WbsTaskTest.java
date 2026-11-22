package org.wonik.coreviaai_be.domain.wbs.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

/**
 * WBS 항목의 도메인 규칙: 상위 기간 자동 산출(FR-WBS-01), 같은 값 판정, 담당 여부.
 */
class WbsTaskTest {

	private static final long PROJECT_ID = 1L;
	private static final long CREATOR_ID = 9L;

	@Test
	@DisplayName("기간 산출: 하위작업의 최소 시작일 ~ 최대 종료일로 상위 작업 기간을 맞춘다(넓히기·좁히기 모두)")
	void deriveScheduleFromSubtasks() {
		final WbsTask parent = task("2026-12-01", "2026-12-31", null);

		parent.deriveScheduleFrom(List.of(
			task("2026-12-05", "2026-12-08", null),
			task("2026-11-30", "2026-12-03", null),
			task("2026-12-06", "2027-01-02", null)));

		assertThat(parent.getStartDate()).isEqualTo("2026-11-30");
		assertThat(parent.getEndDate()).isEqualTo("2027-01-02");

		parent.deriveScheduleFrom(List.of(task("2026-12-10", "2026-12-10", null)));

		assertThat(parent.getStartDate()).isEqualTo("2026-12-10");
		assertThat(parent.getEndDate()).isEqualTo("2026-12-10");
	}

	@Test
	@DisplayName("기간 산출: 하위작업이 없으면(마지막 하위작업을 옮기거나 지움) 지금 기간을 그대로 둔다")
	void deriveScheduleWithoutSubtasks() {
		final WbsTask parent = task("2026-12-01", "2026-12-31", null);

		parent.deriveScheduleFrom(List.of());

		assertThat(parent.getStartDate()).isEqualTo("2026-12-01");
		assertThat(parent.getEndDate()).isEqualTo("2026-12-31");
	}

	@Test
	@DisplayName("같은 값 판정: 예상 공수는 scale이 달라도(3.5와 3.50) 같은 값, null끼리도 같은 값")
	void sameDetails() {
		final WbsTask task = WbsTask.task(PROJECT_ID, 1, details("2026-12-01", "2026-12-05", 10L, new BigDecimal("3.5")),
			WbsItemSource.MANUAL, CREATOR_ID);

		assertThat(task.hasSameDetails(details("2026-12-01", "2026-12-05", 10L, new BigDecimal("3.50")))).isTrue();
		assertThat(task.hasSameDetails(details("2026-12-01", "2026-12-05", 10L, null))).isFalse();
		assertThat(task.hasSameDetails(details("2026-12-01", "2026-12-06", 10L, new BigDecimal("3.5")))).isFalse();
		assertThat(task.hasSameDetails(details("2026-12-01", "2026-12-05", null, new BigDecimal("3.5")))).isFalse();

		final WbsTask noEffort = task("2026-12-01", "2026-12-05", null);
		assertThat(noEffort.hasSameDetails(details("2026-12-01", "2026-12-05", null, null))).isTrue();
	}

	@Test
	@DisplayName("등록 직후 상태는 TODO·진척률 0, 기본 필드 수정은 상태·출처를 바꾸지 않는다")
	void changeDetailsKeepsStatus() {
		final WbsTask task = task("2026-12-01", "2026-12-05", 10L);

		task.changeDetails(details("2026-12-02", "2026-12-09", 11L, new BigDecimal("0.1")));

		assertThat(task.getStatus()).isEqualTo(WbsTaskStatus.TODO);
		assertThat(task.getProgress()).isZero();
		assertThat(task.getSource()).isEqualTo(WbsItemSource.MANUAL);
		assertThat(task.getAssigneeUserId()).isEqualTo(11L);
		assertThat(task.getEndDate()).isEqualTo("2026-12-09");
	}

	@Test
	@DisplayName("담당 여부: 담당자가 없으면 누구의 작업도 아니다")
	void assignedTo() {
		assertThat(task("2026-12-01", "2026-12-05", 10L).isAssignedTo(10L)).isTrue();
		assertThat(task("2026-12-01", "2026-12-05", 10L).isAssignedTo(11L)).isFalse();
		assertThat(task("2026-12-01", "2026-12-05", null).isAssignedTo(10L)).isFalse();
		assertThat(task("2026-12-01", "2026-12-05", null).isAssignedTo(null)).isFalse();
	}

	private static WbsTask task(final String start, final String end, final Long assigneeUserId) {
		return WbsTask.task(PROJECT_ID, 1, details(start, end, assigneeUserId, null), WbsItemSource.MANUAL, CREATOR_ID);
	}

	private static TaskDetails details(final String start, final String end, final Long assigneeUserId,
		final BigDecimal effort) {
		return new TaskDetails("작업", null, assigneeUserId, LocalDate.parse(start), LocalDate.parse(end), effort);
	}
}
