package org.wonik.coreviaai_be.domain.project.structure;

import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;

/**
 * 운영 방식별 기본 구조 생성(FR-PRJ-01 ②). 애자일(FR-PRJ-10)은 구현체를 추가하면 되고
 * 생성 서비스는 바뀌지 않는다(NFR-15의 확장 구조).
 */
public interface ProjectStructureInitializer {

	boolean supports(Methodology methodology);

	/** 프로젝트 저장 직후 같은 트랜잭션에서 호출된다. gateApproverUserId는 미지정이면 null이다. */
	void initialize(Project project, Long gateApproverUserId);
}
