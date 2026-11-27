package org.wonik.coreviaai_be.domain.account.service.result;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.account.entity.AccountRequestStatus;

/**
 * 신청자 본인 상태. 아이디·이름·사용 목적은 다시 돌려주지 않는다.
 */
public record AccountRequestStatusResult(
	AccountRequestStatus status,
	String rejectReason,
	Instant createdAt,
	Instant reviewedAt
) {
}
