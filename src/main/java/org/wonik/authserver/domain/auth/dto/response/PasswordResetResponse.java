package org.wonik.authserver.domain.auth.dto.response;

/**
 * 관리자에게 한 번만 보여 주는 임시 비밀번호. 서버에는 해시만 남는다.
 */
public record PasswordResetResponse(String temporaryPassword) {

	@Override
	public String toString() {
		return "PasswordResetResponse[temporaryPassword=***]";
	}
}
