package org.wonik.coreviaai_be.domain.wbs.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

/**
 * 보드 카드 배치 규칙(FR-TSK-02): 저장된 컬럼, 컬럼이 없을 때 같은 상태 첫 컬럼, 카드 순서, 카드 수.
 */
class BoardLayoutTest {

	private static final long PROJECT_ID = 1L;

	private final BoardColumn todo = column(11L, WbsTaskStatus.TODO, 1);
	private final BoardColumn backlog = column(15L, WbsTaskStatus.TODO, 2);
	private final BoardColumn doing = column(12L, WbsTaskStatus.IN_PROGRESS, 3);
	private final BoardColumn done = column(14L, WbsTaskStatus.DONE, 4);

	@Test
	@DisplayName("카드는 저장된 컬럼에, 컬럼이 비었거나 보드에 없는 컬럼이면 같은 상태의 첫 컬럼에 놓는다")
	void placement() {
		final WbsTask inBacklog = task(1L, "2026-12-05", 15L, WbsTaskStatus.TODO);
		final WbsTask noColumn = task(2L, "2026-12-05", null, WbsTaskStatus.TODO);
		final WbsTask removedColumn = task(3L, "2026-12-05", 99L, WbsTaskStatus.IN_PROGRESS);
		final WbsTask noReviewColumn = task(4L, "2026-12-05", null, WbsTaskStatus.REVIEW);

		final BoardLayout layout = BoardLayout.of(List.of(todo, backlog, doing, done),
			List.of(inBacklog, noColumn, removedColumn, noReviewColumn));

		assertThat(layout.cardsOf(15L)).containsExactly(inBacklog);
		assertThat(layout.cardsOf(11L)).containsExactly(noColumn);
		assertThat(layout.cardsOf(12L)).containsExactly(removedColumn);
		// 상태에 맞는 컬럼이 아예 없으면 보드에 나오지 않는다(상태별 최소 1개 불변식이 막는 경우)
		assertThat(layout.cardCount(11L) + layout.cardCount(15L) + layout.cardCount(12L) + layout.cardCount(14L))
			.isEqualTo(3);
		assertThat(layout.cardsOf(14L)).isEmpty();
		assertThat(layout.cardsOf(999L)).isEmpty();
		assertThat(layout.columns()).extracting(BoardColumn::getId).containsExactly(11L, 15L, 12L, 14L);
	}

	@Test
	@DisplayName("카드 순서는 종료일이 이른 순, 같으면 ID 순(입력 순서와 무관)")
	void cardOrder() {
		final WbsTask late = task(1L, "2026-12-20", 11L, WbsTaskStatus.TODO);
		final WbsTask earlyHighId = task(9L, "2026-12-01", 11L, WbsTaskStatus.TODO);
		final WbsTask earlyLowId = task(3L, "2026-12-01", 11L, WbsTaskStatus.TODO);

		final BoardLayout layout = BoardLayout.of(List.of(todo), List.of(late, earlyHighId, earlyLowId));

		assertThat(layout.cardsOf(11L)).containsExactly(earlyLowId, earlyHighId, late);
		assertThat(layout.cardCount(11L)).isEqualTo(3);
	}

	@Test
	@DisplayName("빈 보드: 컬럼이 없으면 카드도 없고, 작업이 없으면 컬럼 카드 수는 0")
	void empty() {
		assertThat(BoardLayout.of(List.of(), List.of(task(1L, "2026-12-01", null, WbsTaskStatus.TODO))).columns())
			.isEmpty();
		final BoardLayout layout = BoardLayout.of(List.of(todo, doing), List.of());
		assertThat(layout.cardCount(11L)).isZero();
		assertThat(layout.cardsOf(12L)).isEmpty();
	}

	@Test
	@DisplayName("상위 작업 이름: 같은 프로젝트 작업이면 이름, 없거나 null이면 null")
	void taskName() {
		final WbsTask parent = task(1L, "2026-12-01", 11L, WbsTaskStatus.TODO);
		final BoardLayout layout = BoardLayout.of(List.of(todo), List.of(parent));

		assertThat(layout.taskName(1L)).isEqualTo("작업1");
		assertThat(layout.taskName(2L)).isNull();
		assertThat(layout.taskName(null)).isNull();
	}

	private static BoardColumn column(final long id, final WbsTaskStatus status, final int sortOrder) {
		final BoardColumn column = BoardColumn.of(PROJECT_ID, null, status, sortOrder, null);
		ReflectionTestUtils.setField(column, "id", id);
		return column;
	}

	private static WbsTask task(final long id, final String end, final Long columnId, final WbsTaskStatus status) {
		final WbsTask task = WbsTask.task(PROJECT_ID, 1, new TaskDetails("작업" + id, null, null,
			LocalDate.parse("2026-12-01"), LocalDate.parse(end), null), WbsItemSource.MANUAL, 9L);
		ReflectionTestUtils.setField(task, "id", id);
		task.placeOnBoard(columnId);
		if (status != WbsTaskStatus.TODO) {
			final BoardColumn target = BoardColumn.of(PROJECT_ID, null, status, 1, null);
			ReflectionTestUtils.setField(target, "id", columnId == null ? -1L : columnId);
			task.moveToColumn(target, Instant.parse("2026-12-01T00:00:00Z"));
			if (columnId == null) {
				task.placeOnBoard(null);
			}
		}
		return task;
	}
}
