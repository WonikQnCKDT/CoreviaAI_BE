package org.wonik.coreviaai_be.domain.wbs.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.coreviaai_be.domain.wbs.entity.type.DependencyType;
import org.wonik.global.common.entity.BaseTimeEntity;

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
 * 작업 간 선후행 관계(FR-WBS-03). 선행 작업이 끝나야 후행 작업을 시작한다(완료→시작).
 * 지연 파급효과 분석(FR-AIR-04)의 기준 데이터이며, 순환 관계는 등록하는 서비스가 막는다.
 */
@Getter
@Entity
@Table(name = "wbs_dependencies")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WbsDependency extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "predecessor_task_id", nullable = false)
	private Long predecessorTaskId;

	@Column(name = "successor_task_id", nullable = false)
	private Long successorTaskId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "dependency_type", nullable = false, length = 30)
	private DependencyType dependencyType;

	@Column(name = "created_by", nullable = false)
	private Long createdBy;

	private WbsDependency(
		final Long projectId,
		final Long predecessorTaskId,
		final Long successorTaskId,
		final Long createdBy
	) {
		this.projectId = projectId;
		this.predecessorTaskId = predecessorTaskId;
		this.successorTaskId = successorTaskId;
		this.dependencyType = DependencyType.FINISH_TO_START;
		this.createdBy = createdBy;
	}

	public static WbsDependency finishToStart(
		final Long projectId,
		final Long predecessorTaskId,
		final Long successorTaskId,
		final Long createdBy
	) {
		return new WbsDependency(projectId, predecessorTaskId, successorTaskId, createdBy);
	}
}
