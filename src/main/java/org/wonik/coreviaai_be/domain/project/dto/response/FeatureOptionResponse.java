package org.wonik.coreviaai_be.domain.project.dto.response;

import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;

/**
 * ④ 기능 모듈 선택지. enabled가 false면 회색으로 표시하고, requiresPhases이면 단계가 하나 이상일 때만 선택한다.
 */
public record FeatureOptionResponse(ProjectFeature value, boolean enabled, boolean requiresPhases) {

	public static FeatureOptionResponse from(final ProjectFeature feature) {
		return new FeatureOptionResponse(feature, feature.isEnabled(), feature.isRequiresPhases());
	}
}
