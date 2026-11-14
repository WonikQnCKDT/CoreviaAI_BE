package org.wonik.coreviaai_be.domain.project.structure;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.ProjectPhase;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.WaterfallPhaseType;
import org.wonik.coreviaai_be.domain.project.repository.ProjectPhaseRepository;

import lombok.RequiredArgsConstructor;

/**
 * 폭포수 기본 구조: 분석→설계→개발→테스트→이행 5단계와 게이트를 만든다(FR-PRJ-01 인수 기준).
 * 생성 시 지정한 게이트 승인자는 모든 단계 게이트에 적용된다(FR-PRJ-05).
 */
@Component
@RequiredArgsConstructor
public class WaterfallStructureInitializer implements ProjectStructureInitializer {

	private final ProjectPhaseRepository projectPhaseRepository;

	@Override
	public boolean supports(final Methodology methodology) {
		return methodology == Methodology.WATERFALL;
	}

	@Override
	public void initialize(final Project project, final Long gateApproverUserId) {
		final WaterfallPhaseType[] types = WaterfallPhaseType.values();
		final List<ProjectPhase> phases = new ArrayList<>(types.length);
		for (int i = 0; i < types.length; i++) {
			phases.add(ProjectPhase.of(project.getId(), types[i], i + 1, gateApproverUserId));
		}
		projectPhaseRepository.saveAll(phases);
	}
}
