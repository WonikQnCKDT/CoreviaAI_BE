package org.wonik.coreviaai_be.domain.wbs.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskActivityType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 업무 수행 이력(FR-TSK-01 '변경 이력 보관', FR-TSK-06 타임라인의 원천). 추가만 하는 기록이라 수정 시각·버전이 없고
 * 모든 컬럼이 updatable = false다. 기록은 {@link org.wonik.coreviaai_be.domain.wbs.service.TaskHistoryRecorder}만 한다.
 * <p>
 * task_id에는 FK가 없다(V11 주석): 작업이 지워져도 이력(삭제 이력 포함)은 남는다. 다른 모듈과는 ID만 가진다.
 * before_value·after_value는 서버가 정한 필드로 만든 JSON 문자열이다(JSONB 컬럼).
 */
@Getter
@Entity
@Table(name = "task_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "task_id", nullable = false, updatable = false)
	private Long taskId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "activity_type", nullable = false, updatable = false, length = 30)
	private TaskActivityType activityType;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "before_value", updatable = false)
	private String beforeValue;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "after_value", updatable = false)
	private String afterValue;

	/** 수행자. 시스템 처리는 null. */
	@Column(name = "actor_user_id", updatable = false)
	private Long actorUserId;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	private TaskHistory(
		final Long taskId,
		final TaskActivityType activityType,
		final String beforeValue,
		final String afterValue,
		final Long actorUserId,
		final Instant occurredAt
	) {
		this.taskId = taskId;
		this.activityType = activityType;
		this.beforeValue = beforeValue;
		this.afterValue = afterValue;
		this.actorUserId = actorUserId;
		this.occurredAt = occurredAt;
	}

	public static TaskHistory of(
		final Long taskId,
		final TaskActivityType activityType,
		final String beforeValue,
		final String afterValue,
		final Long actorUserId,
		final Instant occurredAt
	) {
		return new TaskHistory(taskId, activityType, beforeValue, afterValue, actorUserId, occurredAt);
	}
}
