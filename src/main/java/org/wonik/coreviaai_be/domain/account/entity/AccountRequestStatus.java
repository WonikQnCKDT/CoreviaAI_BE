package org.wonik.coreviaai_be.domain.account.entity;

/**
 * 계정 신청 상태(FR-ADM-10, account_requests.status).
 * 승인 시 사용자 행을 만드는 것까지가 활성화라서 별도 ACTIVATED 상태는 두지 않는다.
 */
public enum AccountRequestStatus {
	PENDING,
	APPROVED,
	REJECTED
}
