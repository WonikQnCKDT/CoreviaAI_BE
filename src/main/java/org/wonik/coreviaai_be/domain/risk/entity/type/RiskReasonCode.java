package org.wonik.coreviaai_be.domain.risk.entity.type;

/**
 * 감지 사유 코드(risk_detections.reason_code).
 * FR-AIR-01: OVERDUE, PROGRESS_BEHIND, PREDECESSOR_DELAY, MILESTONE_AT_RISK.
 * 나머지 값은 ERD에 맞춰 두고 후속 스토리에서 채운다.
 */
public enum RiskReasonCode {
	OVERDUE,
	PROGRESS_BEHIND,
	PREDECESSOR_DELAY,
	MILESTONE_AT_RISK,
	STATUS_STAY,
	APPROVAL_WAIT,
	ISSUE_REOPEN,
	OVER_ALLOCATION,
	NO_ASSIGNEE,
	NO_ACTIVITY,
	ACTION_ITEM_UNREGISTERED
}
