package org.wonik.coreviaai_be.domain.wbs.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskHistory;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskSnapshot;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskActivityType;
import org.wonik.coreviaai_be.domain.wbs.repository.TaskHistoryRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * 업무 변경 이력(task_histories)을 남기는 유일한 곳(FR-TSK-01 '변경 이력 보관'). WBS 등록·수정·이동·삭제(FR-WBS-01),
 * 업무 상세 수정(FR-TSK-01), AI 초안 반영(FR-PRJ-07)이 모두 이 클래스로 기록한다.
 * <p>
 * 변경과 같은 트랜잭션에서만 호출할 수 있다(MANDATORY): 변경이 롤백되면 이력도 함께 롤백되고, 이력 기록이 실패하면
 * 변경도 커밋되지 않는다. 바뀐 값이 없으면 아무것도 남기지 않는다.
 * <p>
 * before_value·after_value에는 아래 필드만 넣는다(서버가 정한 필드, 사용자 입력 JSON을 그대로 넣지 않음). 날짜는 YYYY-MM-DD.
 * <ul>
 *   <li>CREATED: after = parentTaskId, name, assigneeUserId, startDate, endDate, estimatedEffort, taskType, priority, source</li>
 *   <li>ASSIGNED·UNASSIGNED: assigneeUserId</li>
 *   <li>DATE_CHANGED: startDate, endDate</li>
 *   <li>DETAILS_CHANGED: 바뀐 필드만(name, description, estimatedEffort, taskType, priority, completionCriteria)</li>
 *   <li>MOVED: parentTaskId, sortOrder</li>
 *   <li>STATUS_CHANGED: status, boardColumnId (칸반 카드 이동으로 상태가 바뀜)</li>
 *   <li>BOARD_COLUMN_CHANGED: boardColumnId (같은 상태의 다른 컬럼으로 이동)</li>
 *   <li>PROGRESS_CHANGED: progress (완료 전환 시 100 자동 반영 등)</li>
 *   <li>DELETED: before = CREATED와 같은 필드 + description, completionCriteria, status</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class TaskHistoryRecorder {

	private final TaskHistoryRepository historyRepository;
	private final JsonMapper jsonMapper;
	private final Clock clock;

	/** 등록 이력. 여러 건(AI 초안 반영)은 한 번에 저장한다. id가 정해진(저장된) 작업만 넘긴다. */
	@Transactional(propagation = Propagation.MANDATORY)
	public void created(final Long actorUserId, final Collection<WbsTask> tasks) {
		final Instant now = clock.instant();
		final List<TaskHistory> histories = new ArrayList<>();
		for (final WbsTask task : tasks) {
			histories.add(TaskHistory.of(task.getId(), TaskActivityType.CREATED, null,
				json(creationValues(TaskSnapshot.of(task))), actorUserId, now));
		}
		historyRepository.saveAll(histories);
	}

	/** 바꾸기 전·후 값을 비교해 바뀐 종류마다 한 행씩 남긴다. */
	@Transactional(propagation = Propagation.MANDATORY)
	public void changed(final Long actorUserId, final TaskSnapshot before, final TaskSnapshot after) {
		final Instant now = clock.instant();
		final List<TaskHistory> histories = new ArrayList<>();
		final Long taskId = after.taskId();

		if (!Objects.equals(before.assigneeUserId(), after.assigneeUserId())) {
			final TaskActivityType type = after.assigneeUserId() == null
				? TaskActivityType.UNASSIGNED
				: TaskActivityType.ASSIGNED;
			histories.add(history(taskId, type, values("assigneeUserId", before.assigneeUserId()),
				values("assigneeUserId", after.assigneeUserId()), actorUserId, now));
		}
		if (!before.startDate().equals(after.startDate()) || !before.endDate().equals(after.endDate())) {
			histories.add(history(taskId, TaskActivityType.DATE_CHANGED, schedule(before), schedule(after),
				actorUserId, now));
		}
		final Map<String, Object> detailsBefore = new LinkedHashMap<>();
		final Map<String, Object> detailsAfter = new LinkedHashMap<>();
		diff("name", before.name(), after.name(), detailsBefore, detailsAfter);
		diff("description", before.description(), after.description(), detailsBefore, detailsAfter);
		if (!sameEffort(before.estimatedEffort(), after.estimatedEffort())) {
			detailsBefore.put("estimatedEffort", before.estimatedEffort());
			detailsAfter.put("estimatedEffort", after.estimatedEffort());
		}
		diff("taskType", before.taskType().name(), after.taskType().name(), detailsBefore, detailsAfter);
		diff("priority", before.priority().name(), after.priority().name(), detailsBefore, detailsAfter);
		diff("completionCriteria", before.completionCriteria(), after.completionCriteria(), detailsBefore,
			detailsAfter);
		if (!detailsAfter.isEmpty()) {
			histories.add(history(taskId, TaskActivityType.DETAILS_CHANGED, detailsBefore, detailsAfter, actorUserId,
				now));
		}
		if (!Objects.equals(before.parentTaskId(), after.parentTaskId()) || before.sortOrder() != after.sortOrder()) {
			histories.add(history(taskId, TaskActivityType.MOVED, position(before), position(after), actorUserId, now));
		}
		if (before.status() != after.status()) {
			histories.add(history(taskId, TaskActivityType.STATUS_CHANGED, boardValues(before), boardValues(after),
				actorUserId, now));
		} else if (!Objects.equals(before.boardColumnId(), after.boardColumnId())) {
			histories.add(history(taskId, TaskActivityType.BOARD_COLUMN_CHANGED,
				values("boardColumnId", before.boardColumnId()), values("boardColumnId", after.boardColumnId()),
				actorUserId, now));
		}
		if (before.progress() != after.progress()) {
			histories.add(history(taskId, TaskActivityType.PROGRESS_CHANGED, values("progress", before.progress()),
				values("progress", after.progress()), actorUserId, now));
		}
		if (!histories.isEmpty()) {
			historyRepository.saveAll(histories);
		}
	}

	/** 삭제 이력. 작업 행이 지워진 뒤에도 남는다(task_id FK 없음). */
	@Transactional(propagation = Propagation.MANDATORY)
	public void deleted(final Long actorUserId, final TaskSnapshot before) {
		final Map<String, Object> values = creationValues(before);
		values.put("description", before.description());
		values.put("completionCriteria", before.completionCriteria());
		values.put("status", before.status().name());
		historyRepository.save(TaskHistory.of(before.taskId(), TaskActivityType.DELETED, json(values), null,
			actorUserId, clock.instant()));
	}

	private TaskHistory history(
		final Long taskId,
		final TaskActivityType type,
		final Map<String, Object> before,
		final Map<String, Object> after,
		final Long actorUserId,
		final Instant now
	) {
		return TaskHistory.of(taskId, type, json(before), json(after), actorUserId, now);
	}

	private static Map<String, Object> creationValues(final TaskSnapshot task) {
		final Map<String, Object> values = new LinkedHashMap<>();
		values.put("parentTaskId", task.parentTaskId());
		values.put("name", task.name());
		values.put("assigneeUserId", task.assigneeUserId());
		values.putAll(schedule(task));
		values.put("estimatedEffort", task.estimatedEffort());
		values.put("taskType", task.taskType().name());
		values.put("priority", task.priority().name());
		values.put("source", task.source().name());
		return values;
	}

	private static Map<String, Object> schedule(final TaskSnapshot task) {
		final Map<String, Object> values = new LinkedHashMap<>();
		values.put("startDate", date(task.startDate()));
		values.put("endDate", date(task.endDate()));
		return values;
	}

	private static Map<String, Object> position(final TaskSnapshot task) {
		final Map<String, Object> values = new LinkedHashMap<>();
		values.put("parentTaskId", task.parentTaskId());
		values.put("sortOrder", task.sortOrder());
		return values;
	}

	private static Map<String, Object> boardValues(final TaskSnapshot task) {
		final Map<String, Object> values = new LinkedHashMap<>();
		values.put("status", task.status().name());
		values.put("boardColumnId", task.boardColumnId());
		return values;
	}

	/** 값 하나짜리 맵. null 값도 담는다(Map.of는 null 불가). */
	private static Map<String, Object> values(final String key, final Object value) {
		final Map<String, Object> values = new LinkedHashMap<>();
		values.put(key, value);
		return values;
	}

	private static void diff(
		final String field,
		final Object before,
		final Object after,
		final Map<String, Object> beforeValues,
		final Map<String, Object> afterValues
	) {
		if (!Objects.equals(before, after)) {
			beforeValues.put(field, before);
			afterValues.put(field, after);
		}
	}

	/** NUMERIC(6,1) 값은 읽을 때 scale이 달라질 수 있어(3.5와 3.50) 크기로 비교한다. */
	private static boolean sameEffort(final BigDecimal before, final BigDecimal after) {
		if (before == null || after == null) {
			return before == after;
		}
		return before.compareTo(after) == 0;
	}

	private static String date(final LocalDate date) {
		return date == null ? null : date.toString();
	}

	private String json(final Map<String, Object> values) {
		return values == null ? null : jsonMapper.writeValueAsString(values);
	}
}
