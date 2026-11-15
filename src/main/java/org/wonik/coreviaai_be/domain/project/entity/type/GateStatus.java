package org.wonik.coreviaai_be.domain.project.entity.type;

/**
 * 단계 게이트 판정 상태(FR-PRJ-05). 생성 직후는 PENDING이며 판정은 FR-PRJ-05에서 다룬다.
 */
public enum GateStatus {
	PENDING,
	PASSED,
	REJECTED
}
