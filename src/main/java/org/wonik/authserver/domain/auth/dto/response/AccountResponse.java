package org.wonik.authserver.domain.auth.dto.response;

import org.wonik.authserver.domain.auth.authenticator.AuthenticationOutcome;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;

/**
 * 로그인 응답과 내 계정 조회 응답. mustChangePassword가 true면 FE는 비밀번호 변경 화면으로 보낸다.
 * 프로젝트 역할은 프로젝트 기능(FR-PRJ-04) 구현 시 추가한다.
 */
public record AccountResponse(
	Long userId,
	String loginId,
	String name,
	SystemRole systemRole,
	boolean mustChangePassword
) {

	public static AccountResponse from(final AuthenticationOutcome outcome) {
		return new AccountResponse(
			outcome.userId(),
			outcome.loginId(),
			outcome.name(),
			outcome.systemRole(),
			outcome.mustChangePassword()
		);
	}

	public static AccountResponse from(final User user, final boolean mustChangePassword) {
		return new AccountResponse(
			user.getId(),
			user.getLoginId(),
			user.getName(),
			user.getSystemRole(),
			mustChangePassword
		);
	}
}
