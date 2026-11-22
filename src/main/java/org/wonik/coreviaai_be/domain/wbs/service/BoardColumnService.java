package org.wonik.coreviaai_be.domain.wbs.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.wbs.entity.BoardColumn;
import org.wonik.coreviaai_be.domain.wbs.entity.BoardLayout;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsEditor;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsTaskAccessPolicy;
import org.wonik.coreviaai_be.domain.wbs.repository.BoardColumnRepository;
import org.wonik.coreviaai_be.domain.wbs.service.command.CreateBoardColumnCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.ReorderBoardColumnsCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.UpdateBoardColumnCommand;
import org.wonik.coreviaai_be.domain.wbs.service.result.BoardColumnView;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditChanges;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 칸반 컬럼 관리(FR-TSK-02 '사용자 정의 가능', 3.3 TSK-02: PM만 컬럼·WIP 설정). 추가·이름·WIP 한도 변경·순서 변경·삭제.
 * <p>
 * 불변식:
 * <ul>
 *   <li>상태(TODO·IN_PROGRESS·REVIEW·DONE)마다 컬럼이 1개 이상 있다. 그래서 어느 상태의 카드든 놓을 컬럼이 있다</li>
 *   <li>컬럼의 매핑 상태는 만든 뒤 바꾸지 않는다(카드 상태와 어긋나지 않게)</li>
 *   <li>카드가 있는 컬럼은 지울 수 없다(카드를 먼저 옮긴다). 카드 연쇄 이동·삭제 없음</li>
 *   <li>프로젝트당 컬럼 최대 {@value #MAX_COLUMNS_PER_PROJECT}개(보드를 한 화면에 그리는 크기)</li>
 * </ul>
 * 모든 변경은 프로젝트 행 잠금 안에서 한다(카드 이동·작업 등록과 직렬화). board_columns에는 version이 없어(05 ERD)
 * 같은 컬럼을 두 PM이 고치면 나중 요청이 이긴다. 설정 변경이라 감사 로그를 남긴다(보안 지침: 설정 변경·데이터 삭제).
 * 응답은 변경 후 전체 컬럼 목록(카드 수·WIP 초과 포함, 카드 제외)이다.
 */
@Service
@RequiredArgsConstructor
public class BoardColumnService {

	public static final int MAX_COLUMNS_PER_PROJECT = 20;

	private final WbsTaskAccessPolicy accessPolicy;
	private final BoardColumnRepository columnRepository;
	private final BoardReader boardReader;
	private final AuditLogger auditLogger;

	/** 맨 뒤에 붙인다. */
	@Transactional
	public List<BoardColumnView> create(
		final AuditActor actor,
		final Long projectId,
		final CreateBoardColumnCommand command
	) {
		requirePm(actor, projectId);
		if (columnRepository.countByProjectId(projectId) >= MAX_COLUMNS_PER_PROJECT) {
			throw new CustomException(ErrorCode.BOARD_COLUMN_LIMIT_EXCEEDED);
		}
		final BoardColumn column = columnRepository.save(BoardColumn.of(projectId, command.name(),
			command.statusCategory(), columnRepository.findLastSortOrder(projectId) + 1, command.wipLimit()));
		auditLogger.record(actor, AuditAction.BOARD_COLUMN_CREATED, AuditTargetType.BOARD_COLUMN, column.getId(),
			AuditChanges.create()
				.value("projectId", projectId)
				.value("name", column.getName())
				.value("statusCategory", column.getStatusCategory())
				.value("wipLimit", column.getWipLimit()));
		return columns(projectId);
	}

	/** 이름·WIP 한도를 보낸 값으로 덮어쓴다. 같은 값이면 아무것도 바꾸지 않고 성공한다(멱등). */
	@Transactional
	public List<BoardColumnView> update(
		final AuditActor actor,
		final Long projectId,
		final Long columnId,
		final UpdateBoardColumnCommand command
	) {
		requirePm(actor, projectId);
		final BoardColumn column = getColumn(projectId, columnId);
		if (!column.hasSameSettings(command.name(), command.wipLimit())) {
			final AuditChanges changes = AuditChanges.create()
				.value("projectId", projectId)
				.change("name", column.getName(), command.name())
				.change("wipLimit", column.getWipLimit(), command.wipLimit());
			column.changeSettings(command.name(), command.wipLimit());
			auditLogger.record(actor, AuditAction.BOARD_COLUMN_CHANGED, AuditTargetType.BOARD_COLUMN, columnId, changes);
		}
		return columns(projectId);
	}

	/** 모든 컬럼 ID를 빠짐없이 한 번씩 받아 1부터 다시 매긴다. 같은 순서면 아무것도 바꾸지 않는다. */
	@Transactional
	public List<BoardColumnView> reorder(
		final AuditActor actor,
		final Long projectId,
		final ReorderBoardColumnsCommand command
	) {
		requirePm(actor, projectId);
		final List<BoardColumn> current = columnRepository.findAllByProjectIdOrderBySortOrderAscIdAsc(projectId);
		final List<Long> currentIds = current.stream().map(BoardColumn::getId).toList();
		final List<Long> requested = command.columnIds();
		final Set<Long> distinct = new HashSet<>(requested);
		if (requested.size() != currentIds.size() || distinct.size() != requested.size()
			|| !distinct.containsAll(currentIds)) {
			throw new CustomException(ErrorCode.INVALID_BOARD_COLUMN_ORDER);
		}
		if (!requested.equals(currentIds) || !numberedFromOne(current)) {
			for (final BoardColumn column : current) {
				column.changeSortOrder(requested.indexOf(column.getId()) + 1);
			}
			auditLogger.record(actor, AuditAction.BOARD_COLUMNS_REORDERED, AuditTargetType.PROJECT, projectId,
				AuditChanges.create().change("columnIds", currentIds, requested));
		}
		return columns(projectId);
	}

	/** 상태별 마지막 컬럼이면 409, 카드가 있으면 409. 삭제 후 남은 번호는 다시 매기지 않는다(정렬은 sort_order·id). */
	@Transactional
	public List<BoardColumnView> delete(final AuditActor actor, final Long projectId, final Long columnId) {
		requirePm(actor, projectId);
		final BoardColumn column = getColumn(projectId, columnId);
		if (columnRepository.countByProjectIdAndStatusCategory(projectId, column.getStatusCategory()) <= 1) {
			throw new CustomException(ErrorCode.BOARD_COLUMN_LAST_OF_STATUS);
		}
		if (boardReader.cardCount(column) > 0) {
			throw new CustomException(ErrorCode.BOARD_COLUMN_NOT_EMPTY);
		}
		columnRepository.delete(column);
		columnRepository.flush();
		auditLogger.record(actor, AuditAction.BOARD_COLUMN_DELETED, AuditTargetType.BOARD_COLUMN, columnId,
			AuditChanges.create()
				.value("projectId", projectId)
				.value("name", column.getName())
				.value("statusCategory", column.getStatusCategory())
				.value("wipLimit", column.getWipLimit()));
		return columns(projectId);
	}

	private void requirePm(final AuditActor actor, final Long projectId) {
		final WbsEditor editor = accessPolicy.getEditor(actor.userId(), projectId, WbsTaskAccessPolicy.BOARD_FEATURES);
		editor.requirePm();
	}

	private BoardColumn getColumn(final Long projectId, final Long columnId) {
		return columnRepository.findByIdAndProjectId(columnId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.BOARD_COLUMN_NOT_FOUND));
	}

	private static boolean numberedFromOne(final List<BoardColumn> ordered) {
		for (int i = 0; i < ordered.size(); i++) {
			if (ordered.get(i).getSortOrder() != i + 1) {
				return false;
			}
		}
		return true;
	}

	private List<BoardColumnView> columns(final Long projectId) {
		columnRepository.flush();
		final BoardLayout layout = boardReader.read(projectId);
		return layout.columns().stream()
			.map(column -> new BoardColumnView(column, layout.cardCount(column.getId()), List.of()))
			.toList();
	}
}
