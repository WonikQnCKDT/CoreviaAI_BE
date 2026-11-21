package org.wonik.coreviaai_be.domain.project.dto.response;

import java.util.Arrays;
import java.util.List;

import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;

/**
 * ④ 기능 모듈 선택지. enabled가 false면 회색으로 표시하고, methodologies에 없는 운영 방식에서는 선택을 막는다.
 *
 * @param methodologies 이 모듈을 켤 수 있는 운영 방식(활성 운영 방식 기준)
 */
public record FeatureOptionResponse(ProjectFeature value, boolean enabled, List<Methodology> methodologies) {

	public static FeatureOptionResponse from(final ProjectFeature feature) {
		final List<Methodology> methodologies = Arrays.stream(Methodology.values())
			.filter(Methodology::isEnabled)
			.filter(feature::selectableFor)
			.toList();
		return new FeatureOptionResponse(feature, feature.isEnabled(), methodologies);
	}
}
