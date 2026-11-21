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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 멤버와 역할(FR-PRJ-01 ④, FR-PRJ-04). 한 사람은 한 프로젝트에서 역할 하나만 갖는다(UNIQUE(project_id, user_id)).
 * PM도 actor = PM인 멤버 행이며 프로젝트당 1명 이상이다(복수 PM). 역할 변경 이력은 ProjectMemberHistory에 남긴다.
 */
@Getter
@Entity
@Table(name = "project_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectMember extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "actor", nullable = false, length = 30)
	private ProjectActor actor;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	private ProjectMember(final Long projectId, final Long userId, final ProjectActor actor) {
		this.projectId = projectId;
		this.userId = userId;
		this.actor = actor;
	}

	public static ProjectMember of(final Long projectId, final Long userId, final ProjectActor actor) {
		return new ProjectMember(projectId, userId, actor);
	}

	/** 역할 변경(FR-PRJ-04, PM 추가·해제 포함). 지정 액터 여부·PM 최소 인원 규칙은 ProjectMemberPolicy가 먼저 판정한다. */
	public void changeActor(final ProjectActor actor) {
		this.actor = actor;
	}
}
