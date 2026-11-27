package org.wonik.coreviaai_be.domain.account.dto.response;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.account.entity.AccountRequestStatus;
import org.wonik.coreviaai_be.domain.account.service.result.AccountRequestStatusResult;

public record AccountRequestStatusResponse(
	AccountRequestStatus status,
	String rejectReason,
	Instant createdAt,
	Instant reviewedAt
) {

	public static AccountRequestStatusResponse from(final AccountRequestStatusResult result) {
		return new AccountRequestStatusResponse(
			result.status(),
			result.rejectReason(),
			result.createdAt(),
			result.reviewedAt()
		);
	}
}
