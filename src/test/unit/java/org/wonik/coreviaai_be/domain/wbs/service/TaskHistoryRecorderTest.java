package org.wonik.coreviaai_be.domain.wbs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskHistory;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskSnapshot;
import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskActivityType;
import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskPriority;
import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskType;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;
import org.wonik.coreviaai_be.domain.wbs.repository.TaskHistoryRepository;

import tools.jackson.databind.json.JsonMapper;

/**
 * 변경 전·후 값에서 이력 행을 만드는 규칙(FR-TSK-01 변경 이력). 바뀐 종류마다 한 행, 바뀐 것이 없으면 저장하지 않는다.
 */
class TaskHistoryRecorderTest {

	private static final long ACTOR = 7L;
	private static final Instant NOW = Instant.parse("2026-12-01T00:00:00Z");

	private final TaskHistoryRepository repository = mock(TaskHistoryRepository.class);
	private final TaskHistoryRecorder recorder = new TaskHistoryRecorder(repository, JsonMapper.builder().build(),
		Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	@DisplayName("바뀐 값이 없으면(공수 scale만 다름 포함) 저장하지 않는다")
	void noChange() {
		final TaskSnapshot before = snapshot(10L, "2026-12-01", "2026-12-05", new BigDecimal("3.5"));
		final TaskSnapshot after = snapshot(10L, "2026-12-01", "2026-12-05", new BigDecimal("3.50"));

		recorder.changed(ACTOR, before, after);

		verify(repository, never()).saveAll(any());
	}

	@Test
	@DisplayName("배정·변경은 ASSIGNED, 해제는 UNASSIGNED이고 전·후 담당자를 담는다")
	void assignment() {
		assertThat(recorded(snapshot(null, "2026-12-01", "2026-12-05", null), snapshot(10L, "2026-12-01", "2026-12-05", null)))
			.singleElement().satisfies(history -> {
				assertThat(history.getActivityType()).isEqualTo(TaskActivityType.ASSIGNED);
				assertThat(history.getBeforeValue()).isEqualTo("{\"assigneeUserId\":null}");
				assertThat(history.getAfterValue()).isEqualTo("{\"assigneeUserId\":10}");
				assertThat(history.getActorUserId()).isEqualTo(ACTOR);
				assertThat(history.getOccurredAt()).isEqualTo(NOW);
				assertThat(history.getTaskId()).isEqualTo(1L);
			});
		assertThat(recorded(snapshot(10L, "2026-12-01", "2026-12-05", null), snapshot(null, "2026-12-01", "2026-12-05", null)))
			.singleElement().satisfies(history -> {
				assertThat(history.getActivityType()).isEqualTo(TaskActivityType.UNASSIGNED);
				assertThat(history.getAfterValue()).isEqualTo("{\"assigneeUserId\":null}");
			});
	}

	@Test
	@DisplayName("여러 종류가 함께 바뀌면 종류마다 한 행: 담당자 → 일정 → 상세(바뀐 필드만) 순서")
	void severalKinds() {
		final TaskSnapshot before = snapshot(10L, "2026-12-01", "2026-12-05", null);
		final TaskSnapshot after = new TaskSnapshot(1L, null, 1, "새 이름", null, 11L, LocalDate.parse("2026-12-01"),
			LocalDate.parse("2026-12-09"), new BigDecimal("2.5"), TaskType.TASK, TaskPriority.HIGH, null,
			WbsTaskStatus.TODO, WbsItemSource.MANUAL);

		final List<TaskHistory> histories = recorded(before, after);

		assertThat(histories).extracting(TaskHistory::getActivityType).containsExactly(TaskActivityType.ASSIGNED,
			TaskActivityType.DATE_CHANGED, TaskActivityType.DETAILS_CHANGED);
		assertThat(histories.get(1).getBeforeValue()).isEqualTo("{\"startDate\":\"2026-12-01\",\"endDate\":\"2026-12-05\"}");
		assertThat(histories.get(1).getAfterValue()).isEqualTo("{\"startDate\":\"2026-12-01\",\"endDate\":\"2026-12-09\"}");
		assertThat(histories.get(2).getBeforeValue())
			.isEqualTo("{\"name\":\"작업\",\"estimatedEffort\":null,\"priority\":\"MEDIUM\"}");
		assertThat(histories.get(2).getAfterValue())
			.isEqualTo("{\"name\":\"새 이름\",\"estimatedEffort\":2.5,\"priority\":\"HIGH\"}");
	}

	@Test
	@DisplayName("부모·순서가 바뀌면 MOVED, 상태가 바뀌면 STATUS_CHANGED")
	void moveAndStatus() {
		final TaskSnapshot before = snapshot(null, "2026-12-01", "2026-12-05", null);
		final TaskSnapshot after = new TaskSnapshot(1L, 3L, 2, "작업", null, null, LocalDate.parse("2026-12-01"),
			LocalDate.parse("2026-12-05"), null, TaskType.TASK, TaskPriority.MEDIUM, null, WbsTaskStatus.DONE,
			WbsItemSource.MANUAL);

		final List<TaskHistory> histories = recorded(before, after);

		assertThat(histories).extracting(TaskHistory::getActivityType)
			.containsExactly(TaskActivityType.MOVED, TaskActivityType.STATUS_CHANGED);
		assertThat(histories.get(0).getBeforeValue()).isEqualTo("{\"parentTaskId\":null,\"sortOrder\":1}");
		assertThat(histories.get(0).getAfterValue()).isEqualTo("{\"parentTaskId\":3,\"sortOrder\":2}");
		assertThat(histories.get(1).getAfterValue()).isEqualTo("{\"status\":\"DONE\"}");
	}

	@Test
	@DisplayName("삭제 이력은 before에 삭제 직전 값을 담고 after는 비운다")
	void deleted() {
		recorder.deleted(ACTOR, snapshot(10L, "2026-12-01", "2026-12-05", null));

		final ArgumentCaptor<TaskHistory> captor = ArgumentCaptor.forClass(TaskHistory.class);
		verify(repository).save(captor.capture());
		final TaskHistory history = captor.getValue();
		assertThat(history.getActivityType()).isEqualTo(TaskActivityType.DELETED);
		assertThat(history.getAfterValue()).isNull();
		assertThat(history.getBeforeValue()).contains("\"name\":\"작업\"", "\"assigneeUserId\":10",
			"\"status\":\"TODO\"", "\"completionCriteria\":null");
	}

	@SuppressWarnings("unchecked")
	private List<TaskHistory> recorded(final TaskSnapshot before, final TaskSnapshot after) {
		final TaskHistoryRepository fresh = mock(TaskHistoryRepository.class);
		new TaskHistoryRecorder(fresh, JsonMapper.builder().build(), Clock.fixed(NOW, ZoneOffset.UTC))
			.changed(ACTOR, before, after);
		final ArgumentCaptor<Iterable<TaskHistory>> captor = ArgumentCaptor.forClass(Iterable.class);
		verify(fresh).saveAll(captor.capture());
		final List<TaskHistory> histories = new ArrayList<>();
		captor.getValue().forEach(histories::add);
		return histories;
	}

	private static TaskSnapshot snapshot(final Long assignee, final String start, final String end,
		final BigDecimal effort) {
		return new TaskSnapshot(1L, null, 1, "작업", null, assignee, LocalDate.parse(start), LocalDate.parse(end),
			effort, TaskType.TASK, TaskPriority.MEDIUM, null, WbsTaskStatus.TODO, WbsItemSource.MANUAL);
	}
}
