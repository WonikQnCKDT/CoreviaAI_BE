package org.wonik.authserver.domain.credential.entity;

import java.time.Instant;

import org.wonik.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로컬 ID/PW 로그인 정보(FR-ADM-01). 사용자 1명당 최대 1개이며 users.id를 그대로 키로 쓴다.
 * <p>
 * 사내 SSO로 바꾸면 이 테이블(local_credentials)과 authserver만 걷어내면 된다. 그래서 User 엔티티와
 * JPA 연관관계를 맺지 않고 userId 값만 갖는다(users 쪽이 이 테이블을 몰라도 되도록).
 * 로그인 실패 횟수와 잠금은 동시 시도에서도 정확해야 하므로 비관적 잠금으로 읽은 뒤 이 엔티티의 메서드로만 바꾼다.
 */
@Getter
@Entity
@Table(name = "local_credentials")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LocalCredential extends BaseTimeEntity {

	@Id
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "failed_login_count", nullable = false)
	private int failedLoginCount;

	@Column(name = "locked_at")
	private Instant lockedAt;

	@Column(name = "must_change_password", nullable = false)
	private boolean mustChangePassword;

	@Column(name = "password_changed_at", nullable = false)
	private Instant passwordChangedAt;

	/** 키를 직접 지정하는 엔티티라 버전이 null인지로 신규 여부를 판단한다(Spring Data save 시 persist). */
	@Version
	@Column(name = "version", nullable = false)
	private Long version;

	private LocalCredential(final Long userId, final String passwordHash, final Instant now) {
		this.userId = userId;
		this.passwordHash = passwordHash;
		this.failedLoginCount = 0;
		this.mustChangePassword = true;
		this.passwordChangedAt = now;
	}

	/**
	 * 관리자가 정한 비밀번호로 로그인 정보를 만든다. 본인이 정한 비밀번호가 아니므로 첫 로그인 후 변경해야 한다.
	 */
	public static LocalCredential issue(final Long userId, final String passwordHash, final Instant now) {
		return new LocalCredential(userId, passwordHash, now);
	}

	public boolean isLocked() {
		return lockedAt != null;
	}

	/**
	 * 비밀번호 불일치를 기록한다.
	 *
	 * @return 이번 실패로 잠겼으면 true
	 */
	public boolean recordLoginFailure(final int maxConsecutiveFailures, final Instant now) {
		failedLoginCount++;
		if (!isLocked() && failedLoginCount >= maxConsecutiveFailures) {
			lockedAt = now;
			return true;
		}
		return false;
	}

	public void recordLoginSuccess() {
		failedLoginCount = 0;
	}

	/** 본인이 새 비밀번호를 정한다. 변경 강제 상태가 풀린다. */
	public void changePassword(final String newPasswordHash, final Instant now) {
		passwordHash = newPasswordHash;
		passwordChangedAt = now;
		mustChangePassword = false;
	}

	/** 관리자가 임시 비밀번호로 초기화한다. 잠금이 풀리고 다음 로그인 후 변경해야 한다(UC-ADM-01 2b). */
	public void resetPassword(final String temporaryPasswordHash, final Instant now) {
		passwordHash = temporaryPasswordHash;
		passwordChangedAt = now;
		mustChangePassword = true;
		failedLoginCount = 0;
		lockedAt = null;
	}
}
