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
	DEPARTMENT_DEACTIVATED,
	AI_WBS_DRAFT_REQUESTED,
	AI_WBS_DRAFT_APPLIED,
	PERSONAL_TODO_DELETED,
	CODE_CHANGED,
	I18N_LABEL_CREATED,
	I18N_LABEL_CHANGED,
	/** WBS 작업·하위작업 삭제(FR-WBS-01, 데이터 삭제 감사). 생성·수정 이력은 업무 변경 이력(FR-TSK-01)이 맡는다. */
	WBS_TASK_DELETED,
	/** 마일스톤 삭제(FR-WBS-04, 데이터 삭제 감사). 연결 작업 정보(milestone_tasks)도 함께 지운다. */
	MILESTONE_DELETED,
	/** 칸반 컬럼 추가(FR-TSK-02, 프로젝트 설정 변경 감사). */
	BOARD_COLUMN_CREATED,
	/** 칸반 컬럼 이름·WIP 한도 변경. */
	BOARD_COLUMN_CHANGED,
	/** 칸반 컬럼 순서 변경. 대상은 프로젝트. */
	BOARD_COLUMNS_REORDERED,
	/** 칸반 컬럼 삭제(데이터 삭제 감사). 카드가 있는 컬럼은 지울 수 없다. */
	BOARD_COLUMN_DELETED
}
