package org.wonik.authserver.domain.auth.session;

import java.util.Optional;

/**
 * 로그인 세션 저장소. 운영은 Redis({@link RedisAuthSessionStore}), 테스트는 메모리 구현을 쓴다.
 * 모든 세션은 마지막 활동 후 idle-timeout이 지나면 사라진다.
 */
public interface AuthSessionStore {

	void save(AuthSession session);

	Optional<AuthSession> find(String sessionId);

	/** 사용자 활동이 있었음을 기록해 무활동 만료 시점을 뒤로 민다. */
	void touch(String sessionId);

	void delete(String sessionId);

	/** 비밀번호 변경·초기화 시 그 사용자의 모든 기기 세션을 끊는다. */
	void deleteAllByUserId(Long userId);
}
