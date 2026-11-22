package org.wonik.coreviaai_be.domain.project.structure;

import org.wonik.coreviaai_be.domain.project.entity.Project;

/**
 * 운영 방식과 관계없이 모든 새 프로젝트에 만드는 기본 데이터(예: 칸반 기본 컬럼, FR-TSK-02).
 * 다른 도메인이 구현하고 프로젝트 생성 서비스는 구현체를 모두 호출하기만 한다(프로젝트 도메인이 다른 도메인을 알지 않게).
 */
public interface ProjectDefaultDataInitializer {

	/** 프로젝트 저장 직후 같은 트랜잭션에서 호출된다. 실패하면 프로젝트 생성도 함께 롤백된다. */
	void initialize(Project project);
}
