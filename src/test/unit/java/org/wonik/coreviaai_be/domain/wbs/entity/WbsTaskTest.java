package org.wonik.coreviaai_be.domain.wbs.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskPriority;
import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskType;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

/**
 * WBS 항목의 도메인 규칙: 상위 기간 자동 산출(FR-WBS-01), 같은 값 판정, 담당 여부, 업무 상세 필드·완료 시각(FR-TSK-01).
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

	@Test
	@DisplayName("업무 상세 기본값: 새 작업은 TASK·MEDIUM·완료 조건 없음·완료 시각 없음")
	void defaultAttributes() {
		final WbsTask task = task("2026-12-01", "2026-12-05", null);

		assertThat(task.attributes()).isEqualTo(TaskAttributes.DEFAULT);
		assertThat(task.getTaskType()).isEqualTo(TaskType.TASK);
		assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
		assertThat(task.getCompletionCriteria()).isNull();
		assertThat(task.getCompletedAt()).isNull();
	}

	@Test
	@DisplayName("업무 상세 수정: 유형·우선순위·완료 조건만 바꾸고 기본 필드·상태는 그대로, 같은 값 판정은 세 필드 모두 비교")
	void changeAttributes() {
		final WbsTask task = task("2026-12-01", "2026-12-05", 10L);
		final TaskAttributes defect = new TaskAttributes(TaskType.DEFECT, TaskPriority.HIGH, "재현 안 됨");

		assertThat(task.hasSameAttributes(defect)).isFalse();
		task.changeAttributes(defect);

		assertThat(task.hasSameAttributes(defect)).isTrue();
		assertThat(task.hasSameAttributes(new TaskAttributes(TaskType.DEFECT, TaskPriority.HIGH, null))).isFalse();
		assertThat(task.hasSameAttributes(new TaskAttributes(TaskType.DEFECT, TaskPriority.LOW, "재현 안 됨"))).isFalse();
		assertThat(task.getAssigneeUserId()).isEqualTo(10L);
		assertThat(task.getStatus()).isEqualTo(WbsTaskStatus.TODO);
	}

	@Test
	@DisplayName("완료 시각: 처음 완료한 시각을 남기고 다시 완료해도 바꾸지 않으며, 완료 취소하면 지운다(FR-TSK-02가 사용)")
	void completedAt() {
		final WbsTask task = task("2026-12-01", "2026-12-05", null);
		final Instant first = Instant.parse("2026-12-05T01:00:00Z");

		task.markCompleted(first);
		task.markCompleted(Instant.parse("2026-12-06T01:00:00Z"));
		assertThat(task.getCompletedAt()).isEqualTo(first);

		task.clearCompleted();
		assertThat(task.getCompletedAt()).isNull();
	}

	@Test
	@DisplayName("칸반 이동: 다른 상태 컬럼으로 옮기면 상태·컬럼·상태 변경 시각이 바뀌고, 완료 전이면 완료 시각은 비어 있다")
	void moveToInProgressColumn() {
		final WbsTask task = task("2026-12-01", "2026-12-05", null);
		final Instant now = Instant.parse("2026-12-02T01:00:00Z");

		task.moveToColumn(column(21L, WbsTaskStatus.IN_PROGRESS), now);

		assertThat(task.getStatus()).isEqualTo(WbsTaskStatus.IN_PROGRESS);
		assertThat(task.getBoardColumnId()).isEqualTo(21L);
		assertThat(task.getStatusChangedAt()).isEqualTo(now);
		assertThat(task.getCompletedAt()).isNull();
		assertThat(task.getProgress()).isZero();
	}

	@Test
	@DisplayName("칸반 이동: 완료 컬럼으로 옮기면 완료 시각과 진척률 100을 남기고, 완료에서 벗어나면 완료 시각만 지운다(진척률 유지)")
	void moveToDoneAndBack() {
		final WbsTask task = task("2026-12-01", "2026-12-05", null);
		final Instant done = Instant.parse("2026-12-05T01:00:00Z");

		task.moveToColumn(column(24L, WbsTaskStatus.DONE), done);
		assertThat(task.getStatus()).isEqualTo(WbsTaskStatus.DONE);
		assertThat(task.getCompletedAt()).isEqualTo(done);
		assertThat(task.getProgress()).isEqualTo(WbsTask.COMPLETED_PROGRESS);

		// 같은 상태의 다른 완료 컬럼: 컬럼만 바뀌고 시각은 그대로
		task.moveToColumn(column(25L, WbsTaskStatus.DONE), Instant.parse("2026-12-06T01:00:00Z"));
		assertThat(task.getBoardColumnId()).isEqualTo(25L);
		assertThat(task.getStatusChangedAt()).isEqualTo(done);
		assertThat(task.getCompletedAt()).isEqualTo(done);

		final Instant reopened = Instant.parse("2026-12-07T01:00:00Z");
		task.moveToColumn(column(23L, WbsTaskStatus.REVIEW), reopened);
		assertThat(task.getStatus()).isEqualTo(WbsTaskStatus.REVIEW);
		assertThat(task.getStatusChangedAt()).isEqualTo(reopened);
		assertThat(task.getCompletedAt()).isNull();
		assertThat(task.getProgress()).isEqualTo(WbsTask.COMPLETED_PROGRESS);

		// 다시 완료하면 새 완료 시각
		final Instant doneAgain = Instant.parse("2026-12-08T01:00:00Z");
		task.moveToColumn(column(24L, WbsTaskStatus.DONE), doneAgain);
		assertThat(task.getCompletedAt()).isEqualTo(doneAgain);
	}

	@Test
	@DisplayName("업무 상세 값: 유형·우선순위는 비울 수 없다")
	void attributesRequireTypeAndPriority() {
		assertThatThrownBy(() -> new TaskAttributes(null, TaskPriority.LOW, null))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new TaskAttributes(TaskType.TASK, null, null))
			.isInstanceOf(IllegalArgumentException.class);
	}

	private static BoardColumn column(final long id, final WbsTaskStatus status) {
		final BoardColumn column = BoardColumn.of(PROJECT_ID, null, status, 1, null);
		ReflectionTestUtils.setField(column, "id", id);
		return column;
	}

	private static WbsTask task(final String start, final String end, final Long assigneeUserId) {
		return WbsTask.task(PROJECT_ID, 1, details(start, end, assigneeUserId, null), WbsItemSource.MANUAL, CREATOR_ID);
	}

	private static TaskDetails details(final String start, final String end, final Long assigneeUserId,
		final BigDecimal effort) {
		return new TaskDetails("작업", null, assigneeUserId, LocalDate.parse(start), LocalDate.parse(end), effort);
	}
}
