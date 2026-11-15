package org.wonik.coreviaai_be.domain.project.entity.type;

/**
 * 프로젝트 공개 범위(요구사항 명세서 3.2 공통 규칙). 멤버가 아닌 사용자의 조회는 모두 읽기 전용이다.
 * COMPANY: 전사 공개(전원 읽기) / DEPARTMENT: 주관 부서 소속만 읽기 / MEMBERS_ONLY: 멤버와 소속 범위 경영진만.
 */
public enum ProjectVisibility {
	COMPANY,
	DEPARTMENT,
	MEMBERS_ONLY
}
