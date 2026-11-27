package org.wonik.coreviaai_be.domain.account.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.global.common.entity.BaseTimeEntity;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

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
 * 계정 신청(FR-ADM-10). 승인 전에는 users 행이 없어 로그인할 수 없다.
 * 승인하면 만든 사용자 ID를 연결하고, 반려하면 사유를 남긴다. 행은 지우지 않는다.
 */
@Getter
@Entity
@Table(name = "account_requests")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountRequest extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "login_id", nullable = false, length = 50)
	private String loginId;

	@Column(name = "name", nullable = false, length = 50)
	private String name;

	@Column(name = "department_id")
	private Long departmentId;

	@Column(name = "purpose", nullable = false, length = 500)
	private String purpose;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "status", nullable = false, length = 20)
	private AccountRequestStatus status;

	@Column(name = "reject_reason", length = 500)
	private String rejectReason;

	@Column(name = "reviewed_by")
	private Long reviewedBy;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@Column(name = "user_id")
	private Long userId;

	/** 대기 중 아이디 중복 방지. 승인·반려 후에는 null이라 같은 아이디로 다시 신청할 수 있다. */
	@Column(name = "pending_login_id", length = 50)
	private String pendingLoginId;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	private AccountRequest(final String loginId, final String name, final Long departmentId, final String purpose) {
		this.loginId = loginId;
		this.name = name;
		this.departmentId = departmentId;
		this.purpose = purpose;
		this.status = AccountRequestStatus.PENDING;
		this.pendingLoginId = loginId;
	}

	public static AccountRequest submit(
		final String loginId,
		final String name,
		final Long departmentId,
		final String purpose
	) {
		return new AccountRequest(loginId, name, departmentId, purpose);
	}

	/** 신청자가 조회할 때 아이디와 이름이 모두 같아야 본인 신청으로 본다. */
	public boolean matchesApplicant(final String loginId, final String name) {
		return this.loginId.equals(loginId) && this.name.equals(name);
	}

	/**
	 * 승인. 소속 확인 결과로 부서를 바꿀 수 있다. 사용자 행 생성은 서비스가 맡고 여기서는 연결만 한다.
	 */
	public void approve(
		final Long reviewerId,
		final Long createdUserId,
		final Long confirmedDepartmentId,
		final Instant reviewedAt
	) {
		requirePending();
		this.departmentId = confirmedDepartmentId;
		this.status = AccountRequestStatus.APPROVED;
		this.reviewedBy = reviewerId;
		this.reviewedAt = reviewedAt;
		this.userId = createdUserId;
		this.pendingLoginId = null;
	}

	public void reject(final Long reviewerId, final String reason, final Instant reviewedAt) {
		requirePending();
		if (reason == null || reason.isBlank()) {
			throw new CustomException(ErrorCode.REJECT_REASON_REQUIRED);
		}
		this.status = AccountRequestStatus.REJECTED;
		this.rejectReason = reason.strip();
		this.reviewedBy = reviewerId;
		this.reviewedAt = reviewedAt;
		this.pendingLoginId = null;
	}

	private void requirePending() {
		if (status != AccountRequestStatus.PENDING) {
			throw new CustomException(ErrorCode.ACCOUNT_REQUEST_ALREADY_REVIEWED);
		}
	}
}
