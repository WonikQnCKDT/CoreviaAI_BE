package org.wonik.coreviaai_be.domain.project.entity;

import org.wonik.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 주관 부서(FR-PRJ-01). 한 프로젝트에 0개·1개·여러 개를 지정할 수 있다.
 * 부서 공개 프로젝트는 주관 부서 중 하나에 소속된 사용자가 읽을 수 있다(요구사항 명세서 3.2).
 */
@Getter
@Entity
@Table(name = "project_departments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectDepartment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "project_id", nullable = false)
	private Long projectId;

	@Column(name = "department_id", nullable = false)
	private Long departmentId;

	private ProjectDepartment(final Long projectId, final Long departmentId) {
		this.projectId = projectId;
		this.departmentId = departmentId;
	}

	public static ProjectDepartment of(final Long projectId, final Long departmentId) {
		return new ProjectDepartment(projectId, departmentId);
	}
}
