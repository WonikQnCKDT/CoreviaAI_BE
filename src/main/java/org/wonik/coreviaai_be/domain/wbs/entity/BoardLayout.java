package org.wonik.coreviaai_be.domain.wbs.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

/**
 * 프로젝트 보드의 컬럼과 카드 배치(FR-TSK-02). 저장값을 읽어 메모리에서만 계산하는 순수 규칙이다.
 * <ul>
 *   <li>카드 = 업무 = WBS 작업·하위작업 전부(업무 CRUD FR-TSK-01과 같은 단위)</li>
 *   <li>카드는 board_column_id의 컬럼에 놓인다. 값이 없거나(컬럼 생성 전 데이터) 그 컬럼이 없으면 같은 상태의 첫 컬럼에 놓는다</li>
 *   <li>컬럼 안 카드 순서는 저장하지 않는다. 종료일이 이른 순, 같으면 ID 순</li>
 *   <li>카드 수는 필터와 관계없이 컬럼 전체 기준이고 WIP 초과 판정도 이 수로 한다</li>
 * </ul>
 */
public final class BoardLayout {

	private static final Comparator<WbsTask> CARD_ORDER =
		Comparator.comparing(WbsTask::getEndDate).thenComparing(WbsTask::getId);

	private final List<BoardColumn> columns;
	private final Map<Long, List<WbsTask>> cardsByColumnId;
	private final Map<Long, WbsTask> tasksById;

	private BoardLayout(
		final List<BoardColumn> columns,
		final Map<Long, List<WbsTask>> cardsByColumnId,
		final Map<Long, WbsTask> tasksById
	) {
		this.columns = columns;
		this.cardsByColumnId = cardsByColumnId;
		this.tasksById = tasksById;
	}

	/** columns는 표시 순서대로 받는다. */
	public static BoardLayout of(final List<BoardColumn> columns, final List<WbsTask> tasks) {
		final Map<Long, List<WbsTask>> cards = new LinkedHashMap<>();
		final Map<WbsTaskStatus, Long> firstColumnByStatus = new EnumMap<>(WbsTaskStatus.class);
		for (final BoardColumn column : columns) {
			cards.put(column.getId(), new ArrayList<>());
			firstColumnByStatus.putIfAbsent(column.getStatusCategory(), column.getId());
		}
		final Map<Long, WbsTask> tasksById = new HashMap<>();
		for (final WbsTask task : tasks) {
			tasksById.put(task.getId(), task);
			final Long columnId = cards.containsKey(task.getBoardColumnId())
				? task.getBoardColumnId()
				: firstColumnByStatus.get(task.getStatus());
			if (columnId != null) {
				cards.get(columnId).add(task);
			}
		}
		cards.values().forEach(list -> list.sort(CARD_ORDER));
		return new BoardLayout(List.copyOf(columns), cards, tasksById);
	}

	public List<BoardColumn> columns() {
		return columns;
	}

	/** 컬럼의 카드(표시 순서). 보드에 없는 컬럼이면 빈 목록. */
	public List<WbsTask> cardsOf(final Long columnId) {
		return List.copyOf(cardsByColumnId.getOrDefault(columnId, List.of()));
	}

	public int cardCount(final Long columnId) {
		return cardsByColumnId.getOrDefault(columnId, List.of()).size();
	}

	/** 상위 작업 이름 표시용. 같은 프로젝트에 없으면 null. */
	public String taskName(final Long taskId) {
		final WbsTask task = taskId == null ? null : tasksById.get(taskId);
		return task == null ? null : task.getName();
	}
}
