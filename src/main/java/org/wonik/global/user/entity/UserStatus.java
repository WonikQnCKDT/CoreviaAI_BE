package org.wonik.global.user.entity;

/**
 * 계정 활성 상태. 비활성 계정은 로그인할 수 없다(FR-ADM-02).
 */
public enum UserStatus {
	ACTIVE,
	INACTIVE
}
