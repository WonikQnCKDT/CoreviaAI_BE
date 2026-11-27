package org.wonik.global.audit;

/**
 * 감사 대상 종류. audit_logs.target_id가 어느 테이블의 ID인지 나타낸다.
 */
public enum AuditTargetType {
	USER,
	/** 계정 신청(account_requests.id) */
	ACCOUNT_REQUEST,
	DEPARTMENT,
	PROJECT,
	/** 프로젝트 템플릿(project_templates.id) */
	PROJECT_TEMPLATE,
	PERSONAL_TODO,
	/** 공통 코드(codes.id) */
	CODE,
	/** 다국어 라벨(i18n_labels.id) */
	I18N_LABEL,
	/** WBS 작업·하위작업(wbs_tasks.id) */
	WBS_TASK,
	/** 마일스톤(milestones.id) */
	MILESTONE,
	/** 회의록(meetings.id) */
	MEETING,
	/** 칸반 보드 컬럼(board_columns.id) */
	BOARD_COLUMN
}
