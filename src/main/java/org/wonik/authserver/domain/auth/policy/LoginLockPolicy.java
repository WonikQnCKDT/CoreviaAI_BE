package org.wonik.authserver.domain.auth.policy;

/**
 * 로그인 연속 실패 잠금 기준(FR-ADM-01, NFR-04). 잠금은 시스템 관리자의 비밀번호 초기화로만 풀린다.
 */
public final class LoginLockPolicy {

	public static final int MAX_CONSECUTIVE_FAILURES = 5;

	private LoginLockPolicy() {
	}
}
