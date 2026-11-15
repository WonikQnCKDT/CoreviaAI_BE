package org.wonik.coreviaai_be.domain.project.entity;

import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectStatus;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility;
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
 * 프로젝트(FR-PRJ-01). 운영 방식·부서 유형은 생성 시 확정하고 바꾸지 않는다.
 * <p>
 * pmUserId는 현재 PM(권한 판정 기준)이고 createdBy는 최초 생성자다. 생성 시에는 같지만
 * PM 위임(FR-PRJ-04)으로 pmUserId만 바뀌므로 처음부터 분리해 둔다.
 * 사용자·부서는 다른 모듈 소유라 JPA 연관관계 없이 ID만 가진다(User·Department와 같은 방식).
 * 기간은 특정 날짜의 달력 의미를 갖는 값이라 LocalDate로 모델링한다(시각 아님).
 */
@Getter
@Entity
@Table(name = "projects")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "code", nullable = false, unique = true, length = 20)
	private String code;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDate endDate;

	@Column(name = "department_id", nullable = false)
	private Long departmentId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "visibility", nullable = false, length = 20)
	private ProjectVisibility visibility;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "methodology", nullable = false, length = 20)
	private Methodology methodology;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "department_type", nullable = false, length = 20)
	private DepartmentType departmentType;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "status", nullable = false, length = 20)
	private ProjectStatus status;

	@Column(name = "pm_user_id", nullable = false)
	private Long pmUserId;

	@Column(name = "created_by", nullable = false)
	private Long createdBy;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	private Project(
		final String code,
		final String name,
		final LocalDate startDate,
		final LocalDate endDate,
		final Long departmentId,
		final ProjectVisibility visibility,
		final Methodology methodology,
		final DepartmentType departmentType,
		final Long creatorUserId
	) {
		this.code = code;
		this.name = name;
		this.startDate = startDate;
		this.endDate = endDate;
		this.departmentId = departmentId;
		this.visibility = visibility;
		this.methodology = methodology;
		this.departmentType = departmentType;
		this.status = ProjectStatus.PLANNED;
		this.pmUserId = creatorUserId;
		this.createdBy = creatorUserId;
	}

	/** 생성자가 PM으로 자동 지정된다(요구사항 명세서 3.1). */
	public static Project create(
		final String code,
		final String name,
		final LocalDate startDate,
		final LocalDate endDate,
		final Long departmentId,
		final ProjectVisibility visibility,
		final Methodology methodology,
		final DepartmentType departmentType,
		final Long creatorUserId
	) {
		return new Project(code, name, startDate, endDate, departmentId, visibility, methodology, departmentType, creatorUserId);
	}

	/**
	 * PM 위임·재지정(FR-PRJ-04). 프로젝트 행의 version이 올라가므로 같은 프로젝트의 위임·재지정이 겹치면
	 * 나중 요청이 낙관적 잠금 충돌(409)로 끝난다.
	 */
	public void changePm(final Long newPmUserId) {
		this.pmUserId = newPmUserId;
	}
}
