package org.wonik.coreviaai_be.domain.wbs.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
 * 작업과 단계(project_phases)의 연결은 저장하지 않는다(04. WBS·일정 ERD: 단계 없음 운영 방식 도입으로 phase_id 제거).
 * FR-WBS-01의 '단계' 레벨 표현은 미결이다(SCRUM-107 보고).
 * <p>
 * 하위작업이 있는 작업의 기간(start_date·end_date)은 하위작업 기간의 최소 시작일~최대 종료일로 저장한다(FR-WBS-01 상위 기간 자동 산출).
 * 하위작업이 바뀌는 트랜잭션에서 {@link #deriveScheduleFrom(List)}로 다시 맞추므로, 간트·지연 감지는 저장값을 그대로 읽으면 된다.
 * <p>
 * 다른 모듈(프로젝트·사용자)과는 JPA 연관관계 없이 ID만 가진다(Project와 같은 방식).
 * 기간은 달력 의미를 갖는 날짜라 LocalDate, 예상 공수는 인일 단위(소수 첫째 자리)다.
 */
@Getter
@Entity
@Table(name = "wbs_tasks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WbsTask extends BaseTimeEntity {

	public static final int NAME_MAX_LENGTH = 200;
	public static final int DESCRIPTION_MAX_LENGTH = 2000;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "parent_task_id")
	private Long parentTaskId;

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

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	private WbsTask(
		final Long projectId,
		final Long parentTaskId,
		final int sortOrder,
		final TaskDetails details,
		final WbsItemSource source,
		final Long createdBy
	) {
		this.projectId = projectId;
		this.parentTaskId = parentTaskId;
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
	}

	/** 최상위 작업. */
	public static WbsTask task(
		final Long projectId,
		final int sortOrder,
		final TaskDetails details,
		final WbsItemSource source,
		final Long createdBy
	) {
		return new WbsTask(projectId, null, sortOrder, details, source, createdBy);
	}

	/** 작업 아래 하위작업. */
	public static WbsTask subtask(
		final WbsTask parent,
		final int sortOrder,
		final TaskDetails details,
		final WbsItemSource source,
		final Long createdBy
	) {
		return new WbsTask(parent.projectId, parent.id, sortOrder, details, source, createdBy);
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

	/** 부모와 표시 순서를 바꾼다(드래그 이동). 2단계 계층 규칙은 서비스가 먼저 확인한다. */
	public void moveTo(final Long parentTaskId, final int sortOrder) {
		this.parentTaskId = parentTaskId;
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
