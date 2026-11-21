package org.wonik.support;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.wonik.authserver.domain.auth.session.AuthSession;
import org.wonik.authserver.domain.auth.session.AuthSessionStore;
import org.wonik.authserver.domain.auth.session.AuthSessionProperties;

/**
 * 테스트용 세션 저장소. Redis 대신 주입된 Clock 기준으로 무활동 만료(TTL)를 흉내 낸다.
 */
@Component
@ConditionalOnProperty(name = "app.auth.session-store", havingValue = "memory")
public class InMemoryAuthSessionStore implements AuthSessionStore {

	private final Map<String, StoredSession> sessions = new ConcurrentHashMap<>();
	private final Clock clock;
	private final AuthSessionProperties sessionProperties;

	public InMemoryAuthSessionStore(final Clock clock, final AuthSessionProperties sessionProperties) {
		this.clock = clock;
		this.sessionProperties = sessionProperties;
	}

	@Override
	public void save(final AuthSession session) {
		sessions.put(session.sessionId(), new StoredSession(session, clock.instant().plus(sessionProperties.idleTimeout())));
	}

	@Override
	public Optional<AuthSession> find(final String sessionId) {
		final StoredSession stored = sessions.get(sessionId);
		if (stored == null) {
			return Optional.empty();
		}
		if (!clock.instant().isBefore(stored.expiresAt())) {
			sessions.remove(sessionId);
			return Optional.empty();
		}
		return Optional.of(stored.session());
	}

	@Override
	public void touch(final String sessionId) {
		find(sessionId).ifPresent(session ->
			sessions.put(sessionId, new StoredSession(session, clock.instant().plus(sessionProperties.idleTimeout())))
		);
	}

	@Override
	public void delete(final String sessionId) {
		sessions.remove(sessionId);
	}

	@Override
	public void deleteAllByUserId(final Long userId) {
		sessions.values().removeIf(stored -> stored.session().userId().equals(userId));
	}

	public void clear() {
		sessions.clear();
	}
}
