package org.wonik.coreviaai_be.domain.account.service.result;

import org.wonik.coreviaai_be.domain.account.entity.AccountRequest;

/**
 * 관리자 목록·처리 결과. 부서·처리자 이름은 목록 조회에서 한 번에 채운다.
 */
public record AccountRequestResult(
	AccountRequest request,
	String departmentName,
	String reviewerName
) {
}
