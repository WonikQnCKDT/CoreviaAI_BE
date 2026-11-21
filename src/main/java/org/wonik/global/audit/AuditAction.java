package org.wonik.global.audit;

/**
 * 감사 로그 행위 종류(NFR-07). 값이 늘어도 마이그레이션이 필요 없도록 DB CHECK 제약 없이 이름 그대로 저장한다.
 * 다운로드·AI 요청·AI 제안 승인 등은 해당 기능에서 감사 로그를 도입할 때 추가한다.
 * 저장된 감사 로그를 계속 읽을 수 있도록, 기능이 없어져도 값은 지우지 않는다(지운 기능의 값은 주석으로 표시).
 */
public enum AuditAction {
	USER_CREATED,
	USER_PROFILE_CHANGED,
	USER_ROLE_CHANGED,
	USER_ACTIVATED,
	USER_DEACTIVATED,
	/** 폐지: 프로젝트 생성 권한 부여(최종안에서 생성 권한 제한 폐지). 기존 로그 조회용. */
	PROJECT_CREATION_GRANTED,
	/** 폐지: 프로젝트 생성 권한 회수. 기존 로그 조회용. */
	PROJECT_CREATION_REVOKED,
	PASSWORD_RESET,
	PROJECT_CREATED,
	PROJECT_MEMBER_INVITED,
	PROJECT_MEMBER_ROLE_CHANGED,
	PROJECT_MEMBER_REMOVED,
	PROJECT_PM_ADDED,
	PROJECT_PM_RELEASED,
	/** 폐지: 단일 PM 위임(최종안에서 복수 PM 추가·해제로 대체). 기존 로그 조회용. */
	PROJECT_PM_DELEGATED,
	PROJECT_PM_REASSIGNED,
	DEPARTMENT_CREATED,
	DEPARTMENT_CHANGED,
	DEPARTMENT_ACTIVATED,
	DEPARTMENT_DEACTIVATED
}
