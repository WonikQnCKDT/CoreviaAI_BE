package org.wonik.coreviaai_be.domain.admin.service.result;

/**
 * 관리자 사용자 생성 결과. 임시 비밀번호는 생성 응답에 한 번만 포함하며 저장·로그하지 않는다.
 */
public record AdminUserCreatedResult(AdminUserResult user, String temporaryPassword) {

	@Override
	public String toString() {
		return "AdminUserCreatedResult[user=" + user + ", temporaryPassword=***]";
	}
}
