package org.wonik.coreviaai_be.domain.wbs.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.wbs.entity.BoardColumn;
import org.wonik.coreviaai_be.domain.wbs.entity.BoardLayout;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskSnapshot;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsEditor;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsTaskAccessPolicy;
import org.wonik.coreviaai_be.domain.wbs.repository.BoardColumnRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.service.command.MoveBoardCardCommand;
import org.wonik.coreviaai_be.domain.wbs.service.result.BoardCard;
import org.wonik.coreviaai_be.domain.wbs.service.result.BoardCardMove;
import org.wonik.coreviaai_be.domain.wbs.service.result.BoardColumnView;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 칸반 보드(FR-TSK-02) 조회와 카드 이동(드래그 상태 변경).
 * <p>
 * 권한(3.3 TSK-02): 조회는 프로젝트를 볼 수 있는 사용자 전원(멤버, 공개 범위 열람자, 소속 범위 경영진). 카드 이동은 PM(전체),
 * 그 업무를 담당한 개발자·QA(본인 카드). 현업 담당자·경영진·비멤버는 조회만. 기능 모듈 KANBAN을 끈 프로젝트는 404.
 * <p>
 * 카드 이동 판정 순서: 프로젝트 404 → 기능 404 → 업무 404 → 담당 403 → 컬럼 404 → 같은 컬럼이면 그대로 성공(멱등)
 * → version 409 → 반영. 반영은 프로젝트 행 잠금(WBS 변경·컬럼 관리와 직렬화) 안에서 상태·상태 변경 시각·완료 처리를 바꾸고
 * 같은 트랜잭션에서 이력(STATUS_CHANGED 등)을 남긴 뒤, 옮긴 컬럼의 카드 수(COUNT 쿼리)로 WIP 초과 여부를 돌려준다(경고만, 차단 안 함).
 */
@Service
@RequiredArgsConstructor
public class BoardService {

	private final WbsTaskAccessPolicy accessPolicy;
	private final BoardReader boardReader;
	private final BoardColumnRepository columnRepository;
	private final WbsTaskRepository taskRepository;
	private final TaskHistoryRecorder historyRecorder;
	private final UserNameReader userNameReader;
	private final Clock clock;

	/**
	 * 컬럼(표시 순서)과 컬럼별 카드. assigneeUserId를 주면 그 담당자의 카드만 보이고, 컬럼 카드 수·WIP 초과는 필터와 관계없이 전체 기준이다.
	 * 쿼리는 컬럼·업무·사용자 이름 3회다.
	 */
	@Transactional(readOnly = true)
	public List<BoardColumnView> find(final Long userId, final Long projectId, final Long assigneeUserId) {
		accessPolicy.getReadableProject(userId, projectId, WbsTaskAccessPolicy.BOARD_FEATURES);
		final BoardLayout layout = boardReader.read(projectId);
		final List<List<WbsTask>> visibleCards = layout.columns().stream()
			.map(column -> layout.cardsOf(column.getId()).stream()
				.filter(task -> assigneeUserId == null || task.isAssignedTo(assigneeUserId))
				.toList())
			.toList();
		final Map<Long, String> names = userNameReader.namesOf(visibleCards.stream()
			.flatMap(List::stream)
			.map(WbsTask::getAssigneeUserId)
			.toList());
		final List<BoardColumnView> views = new ArrayList<>(visibleCards.size());
		for (int i = 0; i < visibleCards.size(); i++) {
			final BoardColumn column = layout.columns().get(i);
			views.add(new BoardColumnView(column, layout.cardCount(column.getId()),
				visibleCards.get(i).stream().map(task -> cardOf(layout, task, names)).toList()));
		}
		return views;
	}

	@Transactional
	public BoardCardMove moveCard(
		final AuditActor actor,
		final Long projectId,
		final Long taskId,
		final MoveBoardCardCommand command
	) {
		final WbsEditor editor = accessPolicy.getEditor(actor.userId(), projectId, WbsTaskAccessPolicy.BOARD_FEATURES);
		final WbsTask task = taskRepository.findByIdAndProjectId(taskId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.WBS_TASK_NOT_FOUND));
		editor.requireCanUpdate(task);
		final BoardColumn target = columnRepository.findByIdAndProjectId(command.columnId(), projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.BOARD_COLUMN_NOT_FOUND));

		if (!Objects.equals(task.getBoardColumnId(), target.getId())) {
			if (task.getVersion() != command.version()) {
				throw new CustomException(ErrorCode.CONCURRENT_MODIFICATION);
			}
			final TaskSnapshot before = TaskSnapshot.of(task);
			task.moveToColumn(target, clock.instant());
			taskRepository.flush();
			historyRecorder.changed(actor.userId(), before, TaskSnapshot.of(task));
			if (task.isSubtask() && before.progress() != task.getProgress()) {
				final WbsTask parent = taskRepository.findByIdAndProjectId(task.getParentTaskId(), projectId)
					.orElseThrow(() -> new CustomException(ErrorCode.WBS_TASK_NOT_FOUND));
				final TaskSnapshot parentBefore = TaskSnapshot.of(parent);
				parent.changeProgress(ProgressCalculator.weightedAverage(
					taskRepository.findAllByParentTaskIdOrderBySortOrderAscIdAsc(parent.getId())));
				taskRepository.flush();
				historyRecorder.changed(actor.userId(), parentBefore, TaskSnapshot.of(parent));
			}
		}

		final String parentTaskName = task.getParentTaskId() == null ? null
			: taskRepository.findByIdAndProjectId(task.getParentTaskId(), projectId).map(WbsTask::getName).orElse(null);
		final Map<Long, String> names = userNameReader.namesOf(Collections.singletonList(task.getAssigneeUserId()));
		final BoardCard card = new BoardCard(task, parentTaskName, names.get(task.getAssigneeUserId()));
		return new BoardCardMove(card, new BoardColumnView(target, boardReader.cardCount(target), List.of()));
	}

	private static BoardCard cardOf(final BoardLayout layout, final WbsTask task, final Map<Long, String> names) {
		return new BoardCard(task, layout.taskName(task.getParentTaskId()), names.get(task.getAssigneeUserId()));
	}

}
