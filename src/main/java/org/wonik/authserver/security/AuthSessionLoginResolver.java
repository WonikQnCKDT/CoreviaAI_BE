package org.wonik.authserver.security;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.wonik.authserver.domain.auth.session.AuthSessionStore;
import org.wonik.global.jwt.LoginUser;
import org.wonik.global.jwt.TokenClaims;
import org.wonik.global.security.LoginSession;
import org.wonik.global.security.LoginSessionResolver;

import lombok.RequiredArgsConstructor;

/**
 * authserver 세션 저장소(Redis)로 토큰의 세션을 확인한다. 모든 모듈의 보안 체인이 이 구현을 통해 세션을 본다.
 */
@Component
@RequiredArgsConstructor
public class AuthSessionLoginResolver implements LoginSessionResolver {

	private final AuthSessionStore sessionStore;

	@Override
	public Optional<LoginSession> resolveAndTouch(final TokenClaims claims) {
		final Optional<LoginSession> session = sessionStore.find(claims.sessionId())
			.filter(found -> found.userId().equals(claims.userId()))
			.map(found -> new LoginSession(
				new LoginUser(found.userId(), found.sessionId(), found.systemRole()),
				found.mustChangePassword()
			));
		session.ifPresent(found -> sessionStore.touch(claims.sessionId()));
		return session;
	}
}
