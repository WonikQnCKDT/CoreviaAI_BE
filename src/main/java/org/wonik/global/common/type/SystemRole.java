package org.wonik.global.common.type;

/**
 * 계정 단위 시스템 역할(1인 1역할, 요구사항 명세서 3.1). 프로젝트 역할(PM·개발자 등)과 별개다.
 * <p>
 * 인증 방식(authserver의 로컬 ID/PW, 이후 사내 SSO)과 무관하게 업무 모듈이 권한 판단에 쓰므로 global에 둔다.
 */
public enum SystemRole {
	SYSTEM_ADMIN,
	EXECUTIVE,
	USER;

	/** Spring Security 권한 이름. URL 인가 규칙의 hasRole(...)과 짝을 이룬다. */
	public String authority() {
		return "ROLE_" + name();
	}
}
