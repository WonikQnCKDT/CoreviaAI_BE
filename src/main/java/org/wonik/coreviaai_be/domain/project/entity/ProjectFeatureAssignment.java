package org.wonik.coreviaai_be.domain.project.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
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
 * 프로젝트에서 사용하는 기능 모듈(FR-PRJ-06). 행이 있으면 사용, 없으면 사용 안 함이다.
 * 사용 안 함으로 바꿔도 이 행만 지우고 모듈 데이터는 남긴다(FR-PRJ-14).
 */
@Getter
@Entity
@Table(name = "project_features")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectFeatureAssignment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "feature", nullable = false, length = 30)
	private ProjectFeature feature;

	private ProjectFeatureAssignment(final Long projectId, final ProjectFeature feature) {
		this.projectId = projectId;
		this.feature = feature;
	}

	public static ProjectFeatureAssignment of(final Long projectId, final ProjectFeature feature) {
		return new ProjectFeatureAssignment(projectId, feature);
	}
}
