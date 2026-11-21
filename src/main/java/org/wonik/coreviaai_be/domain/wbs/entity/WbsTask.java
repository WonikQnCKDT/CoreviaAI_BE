package org.wonik.coreviaai_be.domain.wbs.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

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
 * FR-WBS-01의 단계 > 작업 표시 방식은 SCRUM-107에서 정한다.
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
}
