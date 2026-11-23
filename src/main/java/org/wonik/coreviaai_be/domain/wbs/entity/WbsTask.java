package org.wonik.coreviaai_be.domain.wbs.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskPriority;
import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskType;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;
import org.wonik.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * WBS 작업과 하위작업. parentTaskId가 null이면 작업, 있으면 하위작업이다. 하위작업 아래로는 만들지 않는 규칙은
 * 등록하는 서비스가 지킨다.
 * <p>
	 * 작업과 프로젝트 단계는 phase_id로 연결한다. 단계가 없는 프로젝트에서는 null이고, 하위작업은 상위 작업의 단계를 따른다.
 * <p>
 * 하위작업이 있는 작업의 기간(start_date·end_date)은 하위작업 기간의 최소 시작일~최대 종료일로 저장한다(FR-WBS-01 상위 기간 자동 산출).
 * 하위작업이 바뀌는 트랜잭션에서 {@link #deriveScheduleFrom(List)}로 다시 맞추므로, 간트·지연 감지는 저장값을 그대로 읽으면 된다.
 * <p>
 * 다른 모듈(프로젝트·사용자)과는 JPA 연관관계 없이 ID만 가진다(Project와 같은 방식).
 * 기간은 달력 의미를 갖는 날짜라 LocalDate, 예상 공수는 인일 단위(소수 첫째 자리)다.
 * <p>
 * 업무 상세(FR-TSK-01, V11): 유형·우선순위·완료 조건은 {@link TaskAttributes}로 바꾼다.
 * <p>
 * 칸반(FR-TSK-02, V12): 카드는 보드 컬럼(board_column_id)에 놓이고, 상태는 그 컬럼의 매핑 상태와 같게 유지한다.
 * 상태는 {@link #moveToColumn(BoardColumn, Instant)}로만 바뀐다(상태 변경 시각·완료 시각·완료 시 진척률 100 함께 기록).
 */
@Getter
@Entity
@Table(name = "wbs_tasks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WbsTask extends BaseTimeEntity {

	public static final int NAME_MAX_LENGTH = 200;
	public static final int DESCRIPTION_MAX_LENGTH = 2000;
	public static final int COMPLETION_CRITERIA_MAX_LENGTH = 2000;
	/** 완료 상태 전환 시 자동으로 넣는 진척률(FR-TSK-04 인수 기준 '완료 상태 전환 시 100% 자동 반영'). */
	public static final int COMPLETED_PROGRESS = 100;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "parent_task_id")
	private Long parentTaskId;

	@Column(name = "phase_id")
	private Long phaseId;

	/** 같은 부모 안의 표시 순서. 작업은 프로젝트 안, 하위작업은 상위 작업 안에서 매긴다. */
	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	@Column(name = "name", nullable = false, length = NAME_MAX_LENGTH)
	private String name;

	@Column(name = "description", length = DESCRIPTION_MAX_LENGTH)
	private String description;

	@Column(name = "assignee_user_id")
	private Long assigneeUserId;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "estimated_effort", precision = 6, scale = 1)
	private BigDecimal estimatedEffort;

	@Column(name = "progress", nullable = false)
	private int progress;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "status", nullable = false, length = 20)
	private WbsTaskStatus status;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "source", nullable = false, length = 20)
	private WbsItemSource source;

	@Column(name = "created_by", nullable = false)
	private Long createdBy;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "task_type", nullable = false, length = 20)
	private TaskType taskType;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "priority", nullable = false, length = 20)
	private TaskPriority priority;

	@Column(name = "completion_criteria", length = COMPLETION_CRITERIA_MAX_LENGTH)
	private String completionCriteria;

	/** 완료 처리 시각(UTC). 완료되지 않았으면 null. */
	@Column(name = "completed_at")
	private Instant completedAt;

	/** 카드가 놓인 칸반 컬럼. null이면 같은 상태의 첫 컬럼에 놓인 것으로 본다(보드 조회 규칙). */
	@Column(name = "board_column_id")
	private Long boardColumnId;

	/** 마지막 상태 변경 시각(UTC, FR-AIR-02 상태 체류). null이면 등록 이후 상태가 바뀐 적 없다(기준 = created_at). */
	@Column(name = "status_changed_at")
	private Instant statusChangedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	private WbsTask(
		final Long projectId,
		final Long parentTaskId,
		final Long phaseId,
		final int sortOrder,
		final TaskDetails details,
		final WbsItemSource source,
		final Long createdBy
	) {
		this.projectId = projectId;
		this.parentTaskId = parentTaskId;
		this.phaseId = phaseId;
		this.sortOrder = sortOrder;
		this.name = details.name();
		this.description = details.description();
		this.assigneeUserId = details.assigneeUserId();
		this.startDate = details.startDate();
		this.endDate = details.endDate();
		this.estimatedEffort = details.estimatedEffort();
		this.progress = 0;
		this.status = WbsTaskStatus.TODO;
		this.source = source;
		this.createdBy = createdBy;
		this.taskType = TaskAttributes.DEFAULT.taskType();
		this.priority = TaskAttributes.DEFAULT.priority();
		this.completionCriteria = TaskAttributes.DEFAULT.completionCriteria();
	}

	/** 최상위 작업. */
	public static WbsTask task(
		final Long projectId,
		final Long phaseId,
		final int sortOrder,
		final TaskDetails details,
		final WbsItemSource source,
		final Long createdBy
	) {
		return new WbsTask(projectId, null, phaseId, sortOrder, details, source, createdBy);
	}

	/** 단계 연결 도입 전 호출부와 단계 없는 프로젝트용. */
	public static WbsTask task(final Long projectId, final int sortOrder, final TaskDetails details,
		final WbsItemSource source, final Long createdBy) {
		return task(projectId, null, sortOrder, details, source, createdBy);
	}

	/** 작업 아래 하위작업. */
	public static WbsTask subtask(
		final WbsTask parent,
		final int sortOrder,
		final TaskDetails details,
		final WbsItemSource source,
		final Long createdBy
	) {
		return new WbsTask(parent.projectId, parent.id, parent.phaseId, sortOrder, details, source, createdBy);
	}

	public boolean isSubtask() {
		return parentTaskId != null;
	}

	public boolean isAssignedTo(final Long userId) {
		return assigneeUserId != null && assigneeUserId.equals(userId);
	}

	/** 기본 필드(이름·설명·담당자·기간·예상 공수)가 요청 값과 모두 같은지. 같으면 수정 요청을 아무것도 바꾸지 않고 끝낸다. */
	public boolean hasSameDetails(final TaskDetails details) {
		return name.equals(details.name())
			&& Objects.equals(description, details.description())
			&& Objects.equals(assigneeUserId, details.assigneeUserId())
			&& hasSameSchedule(details.startDate(), details.endDate())
			&& sameEffort(estimatedEffort, details.estimatedEffort());
	}

	public boolean hasSameSchedule(final LocalDate start, final LocalDate end) {
		return startDate.equals(start) && endDate.equals(end);
	}

	/** 기본 필드 수정(FR-WBS-01). 권한·기간·담당자 검증은 서비스가 먼저 한다. 상태·진척률·출처는 바꾸지 않는다. */
	public void changeDetails(final TaskDetails details) {
		this.name = details.name();
		this.description = details.description();
		this.assigneeUserId = details.assigneeUserId();
		this.startDate = details.startDate();
		this.endDate = details.endDate();
		this.estimatedEffort = details.estimatedEffort();
	}

	/** 최상위 작업의 단계를 바꾼다. 하위작업 단계는 서비스가 함께 동기화한다. */
	public void changePhase(final Long phaseId) {
		this.phaseId = phaseId;
	}

	/** 직접 입력 또는 하위작업 가중 집계로 진척률을 바꾼다. 입력 범위 검증은 API·서비스가 담당한다. */
	public void changeProgress(final int progress) {
		this.progress = progress;
	}

	public TaskAttributes attributes() {
		return new TaskAttributes(taskType, priority, completionCriteria);
	}

	/** 상세 필드(유형·우선순위·완료 조건)가 요청 값과 모두 같은지. */
	public boolean hasSameAttributes(final TaskAttributes attributes) {
		return attributes().equals(attributes);
	}

	/** 상세 필드 수정(FR-TSK-01). 권한 검증은 서비스가 먼저 한다. */
	public void changeAttributes(final TaskAttributes attributes) {
		this.taskType = attributes.taskType();
		this.priority = attributes.priority();
		this.completionCriteria = attributes.completionCriteria();
	}

	/** 새로 등록한 작업을 처음 놓을 컬럼(대기 상태 컬럼). 컬럼이 없는 프로젝트면 null. */
	public void placeOnBoard(final Long boardColumnId) {
		this.boardColumnId = boardColumnId;
	}

	/**
	 * 카드를 다른 컬럼으로 옮긴다(FR-TSK-02 '카드 드래그 시 상태 변경'). 상태는 컬럼의 매핑 상태가 된다.
	 * 상태가 실제로 바뀔 때만 상태 변경 시각을 남기고, 완료(DONE)가 되면 완료 시각과 진척률 100을,
	 * 완료에서 벗어나면 완료 시각을 지운다(진척률은 그대로 둔다: 진척 입력은 FR-TSK-04 몫).
	 * 같은 상태의 다른 컬럼으로 옮기면 컬럼만 바뀐다. 권한·잠금·컬럼 소속 확인은 서비스가 먼저 한다.
	 */
	public void moveToColumn(final BoardColumn column, final Instant now) {
		this.boardColumnId = column.getId();
		final WbsTaskStatus newStatus = column.getStatusCategory();
		if (status == newStatus) {
			return;
		}
		this.status = newStatus;
		this.statusChangedAt = now;
		if (newStatus == WbsTaskStatus.DONE) {
			markCompleted(now);
			this.progress = COMPLETED_PROGRESS;
		} else {
			clearCompleted();
		}
	}

	/** 완료 처리 시각을 남긴다. 상태 변경(FR-TSK-02)이 DONE으로 바꿀 때 호출한다. 이미 완료 시각이 있으면 그대로 둔다. */
	public void markCompleted(final Instant completedAt) {
		if (this.completedAt == null) {
			this.completedAt = completedAt;
		}
	}

	/** 완료 상태에서 다시 진행으로 돌아가면 완료 시각을 지운다(FR-TSK-02). */
	public void clearCompleted() {
		this.completedAt = null;
	}

	/** 부모와 표시 순서를 바꾼다(드래그 이동). 2단계 계층 규칙은 서비스가 먼저 확인한다. */
	public void moveTo(final Long parentTaskId, final Long phaseId, final int sortOrder) {
		this.parentTaskId = parentTaskId;
		this.phaseId = phaseId;
		this.sortOrder = sortOrder;
	}

	/**
	 * 하위작업 기간으로 이 작업의 기간을 맞춘다(최소 시작일 ~ 최대 종료일). 하위작업이 없으면 지금 값을 그대로 둔다
	 * (마지막 하위작업을 옮기거나 지우면 마지막 산출값이 작업 자신의 기간으로 남는다).
	 */
	public void deriveScheduleFrom(final List<WbsTask> subtasks) {
		if (subtasks.isEmpty()) {
			return;
		}
		LocalDate start = subtasks.get(0).startDate;
		LocalDate end = subtasks.get(0).endDate;
		for (final WbsTask subtask : subtasks) {
			if (subtask.startDate.isBefore(start)) {
				start = subtask.startDate;
			}
			if (subtask.endDate.isAfter(end)) {
				end = subtask.endDate;
			}
		}
		if (!hasSameSchedule(start, end)) {
			this.startDate = start;
			this.endDate = end;
		}
	}

	/** NUMERIC(6,1) 값은 읽을 때 scale이 달라질 수 있어(3.5 와 3.50) 크기로 비교한다. */
	private static boolean sameEffort(final BigDecimal current, final BigDecimal requested) {
		if (current == null || requested == null) {
			return current == requested;
		}
		return current.compareTo(requested) == 0;
	}
}
