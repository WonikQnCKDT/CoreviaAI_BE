package org.wonik.support;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.wonik.coreviaai_be.domain.wbs.drafttoken.DraftTokenOwner;
import org.wonik.coreviaai_be.domain.wbs.drafttoken.WbsAiDraftProperties;
import org.wonik.coreviaai_be.domain.wbs.drafttoken.WbsDraftTokenStore;

/**
 * 테스트용 초안 토큰 저장소. Redis 대신 주입된 Clock 기준으로 TTL을 흉내 낸다.
 */
@Component
@ConditionalOnProperty(name = "app.wbs.ai-draft.token-store", havingValue = "memory")
public class InMemoryWbsDraftTokenStore implements WbsDraftTokenStore {

	private final Map<String, StoredToken> issued = new ConcurrentHashMap<>();
	private final Map<String, StoredToken> applied = new ConcurrentHashMap<>();
	private final Clock clock;
	private final WbsAiDraftProperties properties;

	public InMemoryWbsDraftTokenStore(final Clock clock, final WbsAiDraftProperties properties) {
		this.clock = clock;
		this.properties = properties;
	}

	@Override
	public void issue(final String token, final DraftTokenOwner owner) {
		issued.put(token, new StoredToken(owner, expiry()));
	}

	@Override
	public Optional<DraftTokenOwner> findIssued(final String token) {
		return find(issued, token);
	}

	@Override
	public boolean consume(final String token) {
		final StoredToken removed = issued.remove(token);
		return removed != null && clock.instant().isBefore(removed.expiresAt());
	}

	@Override
	public void markApplied(final String token, final DraftTokenOwner owner) {
		applied.put(token, new StoredToken(owner, expiry()));
	}

	@Override
	public Optional<DraftTokenOwner> findApplied(final String token) {
		return find(applied, token);
	}

	public void clear() {
		issued.clear();
		applied.clear();
	}

	private Optional<DraftTokenOwner> find(final Map<String, StoredToken> tokens, final String token) {
		final StoredToken stored = tokens.get(token);
		if (stored == null || !clock.instant().isBefore(stored.expiresAt())) {
			tokens.remove(token);
			return Optional.empty();
		}
		return Optional.of(stored.owner());
	}

	private Instant expiry() {
		return clock.instant().plus(properties.tokenTtl());
	}
}
