package org.wonik.coreviaai_be.domain.account.dto.response;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.account.entity.AccountRequestStatus;
import org.wonik.coreviaai_be.domain.account.service.result.AccountRequestResult;

/** 신청 직후 응답. 사용 목적과 처리 정보는 관리자 목록에서만 본다. */
public record SubmittedAccountRequestResponse(
	Long id,
	String loginId,
	AccountRequestStatus status,
	Instant createdAt
) {

	public static SubmittedAccountRequestResponse from(final AccountRequestResult result) {
		return new SubmittedAccountRequestResponse(
			result.request().getId(),
			result.request().getLoginId(),
			result.request().getStatus(),
			result.request().getCreatedAt()
		);
	}
}
