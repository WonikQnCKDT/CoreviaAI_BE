package org.wonik.coreviaai_be.domain.wbs.drafttoken;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Redis 초안 토큰 저장소.
 * <ul>
 *   <li>{@code wbs:ai-draft:{token}} (string): "{projectId}:{userId}". TTL = token-ttl</li>
 *   <li>{@code wbs:ai-draft-applied:{token}} (string): 반영 완료 표시. TTL = token-ttl</li>
 * </ul>
 * Redis 장애 시 예외를 그대로 올리고 자동 재시도하지 않는다(세션 저장소와 같은 방식). 연결·명령 timeout은
 * spring.data.redis.* 설정을 따른다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.wbs.ai-draft.token-store", havingValue = "redis", matchIfMissing = true)
public class RedisWbsDraftTokenStore implements WbsDraftTokenStore {

	private static final String ISSUED_KEY_PREFIX = "wbs:ai-draft:";
	private static final String APPLIED_KEY_PREFIX = "wbs:ai-draft-applied:";
	private static final String SEPARATOR = ":";

	private final StringRedisTemplate redisTemplate;
	private final WbsAiDraftProperties properties;

	@Override
	public void issue(final String token, final DraftTokenOwner owner) {
		redisTemplate.opsForValue().set(ISSUED_KEY_PREFIX + token, encode(owner), properties.tokenTtl());
	}

	@Override
	public Optional<DraftTokenOwner> findIssued(final String token) {
		return decode(redisTemplate.opsForValue().get(ISSUED_KEY_PREFIX + token));
	}

	@Override
	public boolean consume(final String token) {
		return Boolean.TRUE.equals(redisTemplate.delete(ISSUED_KEY_PREFIX + token));
	}

	@Override
	public void markApplied(final String token, final DraftTokenOwner owner) {
		redisTemplate.opsForValue().set(APPLIED_KEY_PREFIX + token, encode(owner), properties.tokenTtl());
	}

	@Override
	public Optional<DraftTokenOwner> findApplied(final String token) {
		return decode(redisTemplate.opsForValue().get(APPLIED_KEY_PREFIX + token));
	}

	private static String encode(final DraftTokenOwner owner) {
		return owner.projectId() + SEPARATOR + owner.userId();
	}

	private static Optional<DraftTokenOwner> decode(final String value) {
		if (value == null) {
			return Optional.empty();
		}
		final String[] parts = value.split(SEPARATOR);
		return Optional.of(new DraftTokenOwner(Long.valueOf(parts[0]), Long.valueOf(parts[1])));
	}
}
