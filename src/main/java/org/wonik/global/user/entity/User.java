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
 * authserver와 업무 모듈(coreviaai_be)이 함께 쓰므로 global에 둔다. 부서·직무 등은 FR-ADM-02에서 추가한다.
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

	public boolean isActive() {
		return status == UserStatus.ACTIVE;
	}

	public void recordLogin(final Instant now) {
		lastLoginAt = now;
	}
}
