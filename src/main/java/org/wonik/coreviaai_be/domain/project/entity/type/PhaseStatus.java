package org.wonik.coreviaai_be.domain.project.entity.type;

/**
 * 단계 진행 상태(FR-PRJ-05). 생성 직후는 모두 NOT_STARTED이며 전환은 FR-PRJ-05에서 다룬다.
 */
public enum PhaseStatus {
	NOT_STARTED,
	IN_PROGRESS,
	COMPLETED
}
