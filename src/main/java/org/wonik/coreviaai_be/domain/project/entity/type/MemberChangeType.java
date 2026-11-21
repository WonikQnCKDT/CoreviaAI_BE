package org.wonik.coreviaai_be.domain.project.entity.type;

/**
 * 멤버 이력 종류(FR-PRJ-04). 값을 추가하면 project_member_histories의 CHECK 제약도 새 마이그레이션에서 함께 바꾼다.
 */
public enum MemberChangeType {
	INVITED,
	ROLE_CHANGED,
	REMOVED,
	/** PM이 다른 멤버를 PM으로 추가함(복수 PM, FR-PRJ-04). */
	PM_ADDED,
	/** PM이 PM 한 명을 다른 지정 액터로 바꿈(해제). 프로젝트에 PM이 1명 이상 남아야 한다. */
	PM_RELEASED,
	/** 단일 PM 시절의 위임 이력. 기존 데이터 조회용으로만 남기고 새로 기록하지 않는다. */
	PM_DELEGATED,
	/** 활성 PM이 남지 않아 시스템 관리자가 새 PM을 지정함(요구사항 명세서 3.1 예외). 신규 PM과 제외된 비활성 PM마다 한 행씩 남는다. */
	PM_REASSIGNED
}
