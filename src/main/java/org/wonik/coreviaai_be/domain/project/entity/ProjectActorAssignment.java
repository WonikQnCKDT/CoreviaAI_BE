package org.wonik.coreviaai_be.domain.project.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
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
 * 프로젝트에 지정한 액터(FR-PRJ-01 ③). 여기에 있는 액터만 멤버 초대 시 역할로 선택할 수 있다.
 */
@Getter
@Entity
@Table(name = "project_actors")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectActorAssignment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "actor", nullable = false, length = 30)
	private ProjectActor actor;

	private ProjectActorAssignment(final Long projectId, final ProjectActor actor) {
		this.projectId = projectId;
		this.actor = actor;
	}

	public static ProjectActorAssignment of(final Long projectId, final ProjectActor actor) {
		return new ProjectActorAssignment(projectId, actor);
	}
}
