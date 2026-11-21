package org.wonik.coreviaai_be.domain.wbs.policy;

import org.springframework.stereotype.Component;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.policy.ProjectAccessPolicy;
import org.wonik.coreviaai_be.domain.project.repository.ProjectFeatureAssignmentRepository;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * AI WBS 초안(FR-PRJ-07)을 쓸 수 있는 프로젝트인지 판정한다. 생성과 반영이 같은 규칙을 쓴다.
 * <ol>
 *   <li>PM 중 한 명이어야 한다(3.3 권한 매트릭스 PRJ-07: PM C·A). 볼 수 없으면 404, PM이 아니면 403</li>
 *   <li>폭포수 프로젝트만 허용한다(FR-PRJ-07 '단계별 작업'). 단계 없음 프로젝트는 404 PHASES_NOT_AVAILABLE</li>
 *   <li>WBS 기능 모듈을 켠 프로젝트만 허용한다(FR-PRJ-06: 사용하지 않는 모듈은 메뉴에 없음). 꺼져 있으면 404 FEATURE_DISABLED</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
public class WbsDraftAccessPolicy {

	private final ProjectAccessPolicy accessPolicy;
	private final ProjectFeatureAssignmentRepository featureRepository;

	public Project getDraftableProject(final Long userId, final Long projectId) {
		final Project project = accessPolicy.getManagedProject(userId, projectId);
		if (project.getMethodology() != Methodology.WATERFALL) {
			throw new CustomException(ErrorCode.PHASES_NOT_AVAILABLE);
		}
		if (!featureRepository.existsByProjectIdAndFeature(projectId, ProjectFeature.WBS)) {
			throw new CustomException(ErrorCode.FEATURE_DISABLED);
		}
		return project;
	}
}
