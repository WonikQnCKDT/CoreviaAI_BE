package org.wonik.global.user.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.global.common.entity.BaseTimeEntity;
import org.wonik.global.common.type.SystemRole;

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
 * 사용자(사람·조직 정보). 인증 방식과 무관하게 유지되는 정보만 담는다.
 * <p>
 * 비밀번호·잠금 같은 로컬 로그인 정보는 authserver의 LocalCredential(local_credentials)에 따로 둔다.
 * 인증 서버를 사내 SSO로 바꿔도 이 엔티티와 이를 참조하는 업무 데이터는 그대로 남는다.
 * authserver와 업무 모듈(coreviaai_be)이 함께 쓰므로 global에 둔다.
 * <p>
 * 소속 부서는 1인 1부서이며 JPA 연관관계 없이 ID만 가진다. 시스템 관리자는 부서가 없을 수 있다(그 외 역할의 필수 여부는 API에서 검증).
 * 프로젝트 생성 권한은 역할이 아닌 별도 권한으로, 일반 사용자(USER)만 가질 수 있다(FR-ADM-02, 요구사항 명세서 3.1).
 */
@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "login_id", nullable = false, unique = true, length = 50)
	private String loginId;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "system_role", nullable = false, length = 30)
	private SystemRole systemRole;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "status", nullable = false, length = 20)
	private UserStatus status;

	@Column(name = "department_id")
	private Long departmentId;

	@Column(name = "job_title", length = 50)
	private String jobTitle;

	@Column(name = "can_create_project", nullable = false)
	private boolean canCreateProject;

	@Column(name = "last_login_at")
	private Instant lastLoginAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	private User(final String loginId, final String name, final SystemRole systemRole) {
		this.loginId = loginId;
		this.name = name;
		this.systemRole = systemRole;
		this.status = UserStatus.ACTIVE;
	}

	public static User create(final String loginId, final String name, final SystemRole systemRole) {
		return new User(loginId, name, systemRole);
	}

	/** 관리자 콘솔에서 사용자를 추가할 때 쓴다(FR-ADM-02). 비밀번호는 authserver의 관리자 초기화로 따로 발급한다. */
	public static User create(
		final String loginId,
		final String name,
		final SystemRole systemRole,
		final Long departmentId,
		final String jobTitle
	) {
		final User user = new User(loginId, name, systemRole);
		user.departmentId = departmentId;
		user.jobTitle = jobTitle;
		return user;
	}

	public boolean isActive() {
		return status == UserStatus.ACTIVE;
	}

	public void changeProfile(final String name, final Long departmentId, final String jobTitle) {
		this.name = name;
		this.departmentId = departmentId;
		this.jobTitle = jobTitle;
	}

	/** 일반 사용자가 아닌 역할로 바뀌면 프로젝트 생성 권한도 함께 회수한다. */
	public void changeSystemRole(final SystemRole systemRole) {
		this.systemRole = systemRole;
		if (systemRole != SystemRole.USER) {
			this.canCreateProject = false;
		}
	}

	public void deactivate() {
		this.status = UserStatus.INACTIVE;
	}

	public void activate() {
		this.status = UserStatus.ACTIVE;
	}

	public void grantProjectCreation() {
		if (systemRole != SystemRole.USER) {
			throw new IllegalStateException("프로젝트 생성 권한은 일반 사용자(USER)에게만 부여할 수 있습니다");
		}
		this.canCreateProject = true;
	}

	public void revokeProjectCreation() {
		this.canCreateProject = false;
	}

	public void recordLogin(final Instant now) {
		lastLoginAt = now;
	}
}
