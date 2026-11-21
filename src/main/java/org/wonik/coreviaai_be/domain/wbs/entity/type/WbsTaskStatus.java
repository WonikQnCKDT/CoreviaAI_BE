package org.wonik.coreviaai_be.domain.wbs.entity.type;

/**
 * 작업 상태. 칸반 기본 컬럼(FR-TSK-02: 대기·진행·검토·완료)과 같은 순서다. 새 작업은 TODO로 시작한다.
 */
public enum WbsTaskStatus {
	TODO,
	IN_PROGRESS,
	REVIEW,
	DONE
}
