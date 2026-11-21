package org.wonik.coreviaai_be.domain.wbs.drafttoken;

import java.util.Optional;

/**
 * AI 초안 반영 토큰 저장소(FR-PRJ-07 반영 API의 멱등성). 토큰과 발급 대상, 초안 기록 ID(ai_drafts)만 둔다.
 * 초안 내용은 ai_drafts에 있다. 운영은 Redis({@link RedisWbsDraftTokenStore}), 테스트는 메모리 구현을 쓴다.
 * <ul>
 *   <li>발급(issue): 초안 생성 성공 시. 설정한 TTL이 지나면 사라진다</li>
 *   <li>소비(consume): 반영 직전 원자적으로 지운다. 동시에 두 번 반영되지 않게 한다</li>
 *   <li>반영 표시(markApplied): 반영 성공 후. 재전송을 "만료"가 아닌 "이미 반영"으로 알려 주기 위해 TTL 동안 남긴다</li>
 * </ul>
 */
public interface WbsDraftTokenStore {

	void issue(String token, DraftTokenOwner owner);

	Optional<DraftTokenOwner> findIssued(String token);

	/** 발급 상태의 토큰을 지운다. 이미 없으면(만료·다른 요청이 먼저 소비) false. */
	boolean consume(String token);

	void markApplied(String token, DraftTokenOwner owner);

	Optional<DraftTokenOwner> findApplied(String token);
}
