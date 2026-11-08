package org.wonik.authserver.domain.auth.authenticator;

/**
 * 자격 증명 확인 방식의 경계. 현재 구현은 로컬 ID/PW({@link LocalCredentialAuthenticator}) 하나이며,
 * 이후 사내 인증 연동이 가능해지면 이 인터페이스의 구현만 추가한다(FR-ADM-01 "인증 모듈 분리").
 * 세션·토큰 발급은 구현과 무관하게 AuthService가 맡는다.
 */
public interface CredentialAuthenticator {

	AuthenticationOutcome authenticate(String loginId, String rawPassword, String clientIp);
}
