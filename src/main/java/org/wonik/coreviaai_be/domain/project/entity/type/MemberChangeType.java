package org.wonik.coreviaai_be.domain.project.entity.type;

/**
 * 멤버 이력 종류(FR-PRJ-04). 값을 추가하면 project_member_histories의 CHECK 제약도 새 마이그레이션에서 함께 바꾼다.
 */
public enum MemberChangeType {
	INVITED,
	ROLE_CHANGED,
	REMOVED,
	/** 현 PM이 다른 멤버에게 PM을 넘김. 신규 PM과 기존 PM 두 행이 남는다. */
	PM_DELEGATED,
	/** PM이 비활성이라 시스템 관리자가 새 PM을 지정함(요구사항 명세서 3.1 예외). 신규 PM과 기존 PM 두 행이 남는다. */
	PM_REASSIGNED
}
