package org.wonik.authserver.domain.auth.loginhistory;

/**
 * 접속 로그에 남기는 실패 사유. 사용자 응답은 계정 존재 여부를 드러내지 않도록
 * UNKNOWN_LOGIN_ID와 BAD_PASSWORD를 같은 오류로 돌려주지만, 로그에는 구분해 남긴다.
 */
public enum LoginFailureReason {
	UNKNOWN_LOGIN_ID,
	BAD_PASSWORD,
	ACCOUNT_LOCKED,
	ACCOUNT_DISABLED,
	/** 사용자는 있지만 로컬 로그인 정보가 없음(예: 비밀번호 미발급 계정) */
	NO_LOCAL_CREDENTIAL
}
