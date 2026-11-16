package org.wonik.coreviaai_be.domain.project.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.coreviaai_be.domain.project.entity.type.MemberChangeType;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;

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
 * 멤버 역할 변경 이력(FR-PRJ-04 인수 기준: 역할 변경 이력 보관). 추가만 하는 기록이라 수정 시각·버전이 없다.
 * <p>
 * 감사 로그와 따로 두는 이유: 감사 로그는 1년 보관 후 정리하는 관리자용 기록이고, 이 이력은 프로젝트와 함께 남아
 * PM·멤버가 프로젝트 화면에서 본다. 한 행은 사람 한 명의 역할 변화 한 건이다(초대는 before 없음, 제외는 after 없음).
 */
@Getter
@Entity
@Table(name = "project_member_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMemberHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false, updatable = false)
	private Long projectId;

	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "change_type", nullable = false, updatable = false, length = 20)
	private MemberChangeType changeType;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "before_actor", updatable = false, length = 30)
	private ProjectActor beforeActor;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "after_actor", updatable = false, length = 30)
	private ProjectActor afterActor;

	/** 처리자(PM 또는 PM 재지정 시 시스템 관리자). */
	@Column(name = "changed_by", nullable = false, updatable = false)
	private Long changedBy;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	private ProjectMemberHistory(
		final Long projectId,
		final Long userId,
		final MemberChangeType changeType,
		final ProjectActor beforeActor,
		final ProjectActor afterActor,
		final Long changedBy,
		final Instant occurredAt
	) {
		this.projectId = projectId;
		this.userId = userId;
		this.changeType = changeType;
		this.beforeActor = beforeActor;
		this.afterActor = afterActor;
		this.changedBy = changedBy;
		this.occurredAt = occurredAt;
	}

	public static ProjectMemberHistory of(
		final Long projectId,
		final Long userId,
		final MemberChangeType changeType,
		final ProjectActor beforeActor,
		final ProjectActor afterActor,
		final Long changedBy,
		final Instant occurredAt
	) {
		return new ProjectMemberHistory(projectId, userId, changeType, beforeActor, afterActor, changedBy, occurredAt);
	}
}
