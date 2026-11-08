package org.wonik.authserver.domain.auth.authenticator;

import org.wonik.global.common.type.SystemRole;
import org.wonik.global.common.exception.ErrorCode;

/**
 * 자격 증명 확인 결과. 실패 기록(실패 횟수·잠금·접속 로그)이 롤백되지 않도록
 * 인증기는 예외 대신 이 값을 돌려주고, 호출자가 트랜잭션 밖에서 예외로 바꾼다.
 */
public record AuthenticationOutcome(
	boolean success,
	Long userId,
	String loginId,
	String name,
	SystemRole systemRole,
	boolean mustChangePassword,
	ErrorCode failureCode
) {

	public static AuthenticationOutcome succeeded(
		final Long userId,
		final String loginId,
		final String name,
		final SystemRole systemRole,
		final boolean mustChangePassword
	) {
		return new AuthenticationOutcome(true, userId, loginId, name, systemRole, mustChangePassword, null);
	}

	public static AuthenticationOutcome failed(final ErrorCode failureCode) {
		return new AuthenticationOutcome(false, null, null, null, null, false, failureCode);
	}
}
