package org.wonik.authserver.domain.auth.loginhistory;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
 * 로그인 시도 기록(UC-ADM-01 4단계·2a, NFR-07). 수정·삭제하지 않는 append-only 로그다.
 * 조회 화면과 보존 기간 정리 작업은 FR-ADM-04에서 구현한다. 비밀번호는 어떤 형태로도 남기지 않는다.
 */
@Getter
@Entity
@Table(name = "login_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginHistory {

	private static final int LOGIN_ID_MAX_LENGTH = 50;
	private static final int IP_MAX_LENGTH = 45;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", updatable = false)
	private Long userId;

	@Column(name = "login_id", nullable = false, updatable = false, length = LOGIN_ID_MAX_LENGTH)
	private String loginId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "result", nullable = false, updatable = false, length = 20)
	private LoginResult result;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "failure_reason", updatable = false, length = 30)
	private LoginFailureReason failureReason;

	@Column(name = "ip_address", nullable = false, updatable = false, length = IP_MAX_LENGTH)
	private String ipAddress;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	private LoginHistory(
		final Long userId,
		final String loginId,
		final LoginResult result,
		final LoginFailureReason failureReason,
		final String ipAddress,
		final Instant occurredAt
	) {
		this.userId = userId;
		this.loginId = truncate(loginId, LOGIN_ID_MAX_LENGTH);
		this.result = result;
		this.failureReason = failureReason;
		this.ipAddress = truncate(ipAddress, IP_MAX_LENGTH);
		this.occurredAt = occurredAt;
	}

	public static LoginHistory success(
		final Long userId,
		final String loginId,
		final String ipAddress,
		final Instant occurredAt
	) {
		return new LoginHistory(userId, loginId, LoginResult.SUCCESS, null, ipAddress, occurredAt);
	}

	public static LoginHistory failure(
		final Long userId,
		final String loginId,
		final LoginFailureReason reason,
		final String ipAddress,
		final Instant occurredAt
	) {
		return new LoginHistory(userId, loginId, LoginResult.FAILURE, reason, ipAddress, occurredAt);
	}

	private static String truncate(final String value, final int maxLength) {
		if (value == null) {
			return "";
		}
		return value.length() <= maxLength ? value : value.substring(0, maxLength);
	}
}
