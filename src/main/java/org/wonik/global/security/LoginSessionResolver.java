package org.wonik.global.security;

import java.util.Optional;

import org.wonik.global.jwt.TokenClaims;

/**
 * 검증된 토큰이 가리키는 세션을 찾고, 있으면 활동 시각을 갱신한다.
 * <p>
 * 업무 모듈의 보안 체인은 이 인터페이스만 알고 세션 저장 방식(authserver의 Redis 등)은 모른다.
 * 인증 서버를 사내 SSO로 바꾸면 이 구현만 교체한다.
 */
public interface LoginSessionResolver {

	/**
	 * @return 세션이 없거나(만료·로그아웃) 토큰 사용자와 다르면 빈 값
	 * @throws org.springframework.dao.DataAccessException 세션 저장소에 연결할 수 없을 때
	 */
	Optional<LoginSession> resolveAndTouch(TokenClaims claims);
}
