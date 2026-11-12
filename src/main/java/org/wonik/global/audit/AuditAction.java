package org.wonik.global.audit;

/**
 * 감사 로그 행위 종류(NFR-07). 값이 늘어도 마이그레이션이 필요 없도록 DB CHECK 제약 없이 이름 그대로 저장한다.
 * 다운로드·AI 요청·AI 제안 승인 등은 해당 기능에서 감사 로그를 도입할 때 추가한다.
 */
public enum AuditAction {
	USER_CREATED,
	USER_PROFILE_CHANGED,
	USER_ROLE_CHANGED,
	USER_ACTIVATED,
	USER_DEACTIVATED,
	PROJECT_CREATION_GRANTED,
	PROJECT_CREATION_REVOKED,
	PASSWORD_RESET,
	DEPARTMENT_CREATED,
	DEPARTMENT_CHANGED,
	DEPARTMENT_ACTIVATED,
	DEPARTMENT_DEACTIVATED
}
