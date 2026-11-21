package org.wonik.coreviaai_be.domain.project.structure;

import org.springframework.stereotype.Component;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;

/**
 * 단계 없음 운영 방식(FR-PRJ-01 ②): 단계·게이트를 만들지 않고 업무 목록으로 운영한다.
 * 게이트 승인자는 정책에서 이미 거부하므로 여기서는 받지 않는다.
 */
@Component
public class NoPhaseStructureInitializer implements ProjectStructureInitializer {

	@Override
	public boolean supports(final Methodology methodology) {
		return methodology == Methodology.NONE;
	}

	@Override
	public void initialize(final Project project, final Long gateApproverUserId) {
		// 단계 없음 프로젝트는 만들 기본 구조가 없다.
	}
}
