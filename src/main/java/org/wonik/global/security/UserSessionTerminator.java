package org.wonik.global.security;

/**
 * 한 사용자의 로그인 세션을 모두 끊는다.
 * <p>
 * 세션은 시스템 역할을 저장해 두고 매 요청 그 값으로 인가하므로, 관리자가 역할을 바꾸거나 계정을 비활성화하면
 * 기존 세션을 끊어야 바뀐 권한이 즉시 적용된다(FR-ADM-02). 업무 모듈은 이 인터페이스만 알고 세션 저장 방식은 모른다.
 * 인증 서버를 사내 SSO로 바꾸면 이 구현만 교체한다({@link LoginSessionResolver}와 같은 방식).
 */
public interface UserSessionTerminator {

	/**
	 * DB 트랜잭션이 커밋된 뒤 호출한다(세션 저장소 호출이 DB 트랜잭션을 잡고 있지 않도록).
	 *
	 * @throws org.springframework.dao.DataAccessException 세션 저장소에 연결할 수 없을 때
	 */
	void terminateAllSessions(Long userId);
}
