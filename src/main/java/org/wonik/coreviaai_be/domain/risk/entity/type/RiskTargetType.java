package org.wonik.coreviaai_be.domain.risk.entity.type;

/** 감지 대상 종류(risk_detections.target_type). FR-AIR-01은 TASK·MILESTONE만 쓴다. */
public enum RiskTargetType {
	TASK,
	MILESTONE,
	DELIVERABLE,
	ISSUE,
	USER,
	ACTION_ITEM
}
