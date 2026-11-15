package org.wonik.coreviaai_be.domain.project.entity.type;

/**
 * 프로젝트 진행 상태. 생성 직후는 PLANNED이며 상태 전환 API는 후속 이슈에서 다룬다.
 * 명세에 상태 값 정의가 없어 팀 확정 전 초안이다(목록 필터 기준, FR-PRJ-01).
 */
public enum ProjectStatus {
	PLANNED,
	IN_PROGRESS,
	ON_HOLD,
	COMPLETED,
	CANCELLED
}
