package org.wonik.coreviaai_be.domain.project.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectSettingChangeType;

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
 * 프로젝트 설정 변경 이력(FR-PRJ-14). 추가만 하는 기록이라 수정 시각·버전이 없고 컬럼은 updatable = false다.
 * 감사 로그는 보관 기간 뒤 정리되지만, 이 이력은 프로젝트 화면에서 변경 전후를 보는 용도라 따로 둔다.
 */
@Getter
@Entity
@Table(name = "project_setting_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectSettingHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false, updatable = false)
	private Long projectId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "change_type", nullable = false, updatable = false, length = 30)
	private ProjectSettingChangeType changeType;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "before_value", updatable = false)
	private String beforeValue;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "after_value", updatable = false)
	private String afterValue;

	@Column(name = "changed_by", nullable = false, updatable = false)
	private Long changedBy;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	private ProjectSettingHistory(
		final Long projectId,
		final ProjectSettingChangeType changeType,
		final String beforeValue,
		final String afterValue,
		final Long changedBy,
		final Instant occurredAt
	) {
		this.projectId = projectId;
		this.changeType = changeType;
		this.beforeValue = beforeValue;
		this.afterValue = afterValue;
		this.changedBy = changedBy;
		this.occurredAt = occurredAt;
	}

	public static ProjectSettingHistory of(
		final Long projectId,
		final ProjectSettingChangeType changeType,
		final String beforeValue,
		final String afterValue,
		final Long changedBy,
		final Instant occurredAt
	) {
		return new ProjectSettingHistory(projectId, changeType, beforeValue, afterValue, changedBy, occurredAt);
	}
}
