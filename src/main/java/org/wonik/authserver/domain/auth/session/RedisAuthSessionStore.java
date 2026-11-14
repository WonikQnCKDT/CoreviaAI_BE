package org.wonik.authserver.domain.auth.session;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.wonik.global.jwt.JwtProperties;
import org.wonik.global.common.type.SystemRole;

import lombok.RequiredArgsConstructor;

/**
 * Redis 세션 저장소.
 * <ul>
 *   <li>{@code auth:session:{sessionId}} (hash): 세션 내용. TTL = idle-timeout, 활동마다 갱신</li>
 *   <li>{@code auth:user-sessions:{userId}} (set): 사용자별 세션 ID 목록. 전체 세션 강제 만료용</li>
 * </ul>
 * Redis 장애 시 예외를 그대로 올리며 자동 재시도하지 않는다(인증 판정을 추측으로 대신하지 않기 위해).
 * 연결·명령 timeout은 application.properties의 spring.data.redis.* 값을 따른다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.auth.session-store", havingValue = "redis", matchIfMissing = true)
public class RedisAuthSessionStore implements AuthSessionStore {

	private static final String SESSION_KEY_PREFIX = "auth:session:";
	private static final String USER_SESSIONS_KEY_PREFIX = "auth:user-sessions:";
	private static final String FIELD_USER_ID = "userId";
	private static final String FIELD_ROLE = "systemRole";
	private static final String FIELD_MUST_CHANGE_PASSWORD = "mustChangePassword";
	private static final String FIELD_CREATED_AT = "createdAt";

	private final StringRedisTemplate redisTemplate;
	private final AuthSessionProperties sessionProperties;
	private final JwtProperties jwtProperties;

	@Override
	public void save(final AuthSession session) {
		final String sessionKey = sessionKey(session.sessionId());
		redisTemplate.opsForHash().putAll(sessionKey, Map.of(
			FIELD_USER_ID, String.valueOf(session.userId()),
			FIELD_ROLE, session.systemRole().name(),
			FIELD_MUST_CHANGE_PASSWORD, String.valueOf(session.mustChangePassword()),
			FIELD_CREATED_AT, session.createdAt().toString()
		));
		redisTemplate.expire(sessionKey, sessionProperties.idleTimeout());

		final String userSessionsKey = userSessionsKey(session.userId());
		redisTemplate.opsForSet().add(userSessionsKey, session.sessionId());
		redisTemplate.expire(userSessionsKey, jwtProperties.refreshTokenTtl());
	}

	@Override
	public Optional<AuthSession> find(final String sessionId) {
		final Map<Object, Object> fields = redisTemplate.opsForHash().entries(sessionKey(sessionId));
		if (fields.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(new AuthSession(
			sessionId,
			Long.valueOf((String)fields.get(FIELD_USER_ID)),
			SystemRole.valueOf((String)fields.get(FIELD_ROLE)),
			Boolean.parseBoolean((String)fields.get(FIELD_MUST_CHANGE_PASSWORD)),
			Instant.parse((String)fields.get(FIELD_CREATED_AT))
		));
	}

	@Override
	public void touch(final String sessionId) {
		redisTemplate.expire(sessionKey(sessionId), sessionProperties.idleTimeout());
	}

	@Override
	public void delete(final String sessionId) {
		find(sessionId).ifPresent(session ->
			redisTemplate.opsForSet().remove(userSessionsKey(session.userId()), sessionId)
		);
		redisTemplate.delete(sessionKey(sessionId));
	}

	@Override
	public void deleteAllByUserId(final Long userId) {
		final String userSessionsKey = userSessionsKey(userId);
		final Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);
		if (sessionIds != null && !sessionIds.isEmpty()) {
			redisTemplate.delete(sessionIds.stream().map(RedisAuthSessionStore::sessionKey).toList());
		}
		redisTemplate.delete(userSessionsKey);
	}

	private static String sessionKey(final String sessionId) {
		return SESSION_KEY_PREFIX + sessionId;
	}

	private static String userSessionsKey(final Long userId) {
		return USER_SESSIONS_KEY_PREFIX + userId;
	}
}
