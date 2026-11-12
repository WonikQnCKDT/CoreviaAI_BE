package org.wonik.global.department.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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
 * 부서(조직). parentId로 상위·하위 계층을 표현하며, 상위가 없으면 최상위 부서다.
 * <p>
 * 사용자(User)의 소속과 이후 프로젝트 주관 부서·부서 공개 범위의 기준이 되므로 global에 둔다.
 * 상위 부서는 JPA 연관관계 없이 ID만 가진다(계층 검증은 서비스에서 한다).
 */
@Getter
@Entity
@Table(name = "departments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "parent_id")
	private Long parentId;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "status", nullable = false, length = 20)
	private DepartmentStatus status;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	private Department(final Long parentId, final String name) {
		this.parentId = parentId;
		this.name = name;
		this.status = DepartmentStatus.ACTIVE;
	}

	public static Department create(final Long parentId, final String name) {
		return new Department(parentId, name);
	}

	public boolean isActive() {
		return status == DepartmentStatus.ACTIVE;
	}

	public void rename(final String name) {
		this.name = name;
	}

	public void moveTo(final Long parentId) {
		this.parentId = parentId;
	}

	public void deactivate() {
		this.status = DepartmentStatus.INACTIVE;
	}

	public void activate() {
		this.status = DepartmentStatus.ACTIVE;
	}
}
