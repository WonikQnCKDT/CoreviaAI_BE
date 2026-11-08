package org.wonik.authserver.domain.auth.session;

import java.time.Instant;

import org.wonik.global.common.type.SystemRole;

/**
 * 서버 측 로그인 세션. JWT는 이 세션을 가리키는 열쇠일 뿐이고, 무활동 만료(30분)·로그아웃·강제 만료는
 * 세션 저장소에서 판정한다. 역할과 비밀번호 변경 강제 여부도 매 요청 이 값을 기준으로 본다.
 */
public record AuthSession(
	String sessionId,
	Long userId,
	SystemRole systemRole,
	boolean mustChangePassword,
	Instant createdAt
) {
}
