package org.wonik.coreviaai_be.domain.account.dto.response;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.account.entity.AccountRequest;
import org.wonik.coreviaai_be.domain.account.entity.AccountRequestStatus;
import org.wonik.coreviaai_be.domain.account.service.result.AccountRequestResult;

public record AccountRequestResponse(
	Long id,
	String loginId,
	String name,
	Long departmentId,
	String departmentName,
	String purpose,
	AccountRequestStatus status,
	String rejectReason,
	Long reviewedBy,
	String reviewerName,
	Instant reviewedAt,
	Long userId,
	long version,
	Instant createdAt,
	Instant updatedAt
) {

	public static AccountRequestResponse from(final AccountRequestResult result) {
		final AccountRequest request = result.request();
		return new AccountRequestResponse(
			request.getId(),
			request.getLoginId(),
			request.getName(),
			request.getDepartmentId(),
			result.departmentName(),
			request.getPurpose(),
			request.getStatus(),
			request.getRejectReason(),
			request.getReviewedBy(),
			result.reviewerName(),
			request.getReviewedAt(),
			request.getUserId(),
			request.getVersion(),
			request.getCreatedAt(),
			request.getUpdatedAt()
		);
	}
}
