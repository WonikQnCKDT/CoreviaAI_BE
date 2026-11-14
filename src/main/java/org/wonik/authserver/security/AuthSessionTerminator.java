package org.wonik.authserver.security;

import org.springframework.stereotype.Component;
import org.wonik.authserver.domain.auth.session.AuthSessionStore;
import org.wonik.global.security.UserSessionTerminator;

import lombok.RequiredArgsConstructor;

/**
 * authserver 세션 저장소(Redis)에서 사용자의 모든 세션을 지운다. 업무 모듈의 사용자 관리가 이 구현을 통해 세션을 끊는다.
 */
@Component
@RequiredArgsConstructor
public class AuthSessionTerminator implements UserSessionTerminator {

	private final AuthSessionStore sessionStore;

	@Override
	public void terminateAllSessions(final Long userId) {
		sessionStore.deleteAllByUserId(userId);
	}
}
