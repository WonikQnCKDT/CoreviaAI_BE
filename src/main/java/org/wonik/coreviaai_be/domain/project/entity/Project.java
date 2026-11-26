package org.wonik.coreviaai_be.domain.project.entity;

import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectStatus;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectTemplate;
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
 * 프로젝트(FR-PRJ-01). 기간·공개 범위·운영 방식은 생성 후에도 PM이 바꿀 수 있다(FR-PRJ-14).
 * <p>
 * PM은 복수로 지정할 수 있어(FR-PRJ-04) 프로젝트 행이 아니라 PM 역할 멤버(project_members.actor = PM)로 관리한다.
 * createdBy는 최초 생성자 기록일 뿐 권한 판정에 쓰지 않는다. 주관 부서(0~N개)는 ProjectDepartment,
 * 기능 구성은 ProjectFeatureAssignment가 맡는다. 사용자·부서는 다른 모듈 소유라 JPA 연관관계 없이 ID만 가진다.
 * 기간은 달력 의미를 갖는 날짜라 LocalDate로 모델링하고, 생성 후 입력할 수 있어 비어 있을 수 있다.
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

	@Column(name = "start_date")
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "visibility", nullable = false, length = 20)
	private ProjectVisibility visibility;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "template", nullable = false, length = 20)
	private ProjectTemplate template;

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
		final ProjectVisibility visibility,
		final ProjectTemplate template,
		final Methodology methodology,
		final DepartmentType departmentType,
		final Long creatorUserId
	) {
		this.code = code;
		this.name = name;
		this.startDate = startDate;
		this.endDate = endDate;
		this.visibility = visibility;
		this.template = template;
		this.methodology = methodology;
		this.departmentType = departmentType;
		this.status = ProjectStatus.PLANNED;
		this.createdBy = creatorUserId;
	}

	/** 생성자를 PM 역할 멤버로 등록하는 일은 생성 서비스가 같은 트랜잭션에서 한다(요구사항 명세서 3.1). */
	public static Project create(
		final String code,
		final String name,
		final LocalDate startDate,
		final LocalDate endDate,
		final ProjectVisibility visibility,
		final ProjectTemplate template,
		final Methodology methodology,
		final DepartmentType departmentType,
		final Long creatorUserId
	) {
		return new Project(code, name, startDate, endDate, visibility, template, methodology, departmentType,
			creatorUserId);
	}

	public void changeBasicInfo(final LocalDate startDate, final LocalDate endDate, final ProjectVisibility visibility) {
		this.startDate = startDate;
		this.endDate = endDate;
		this.visibility = visibility;
	}

	public void changeMethodology(final Methodology methodology) {
		this.methodology = methodology;
	}
}
