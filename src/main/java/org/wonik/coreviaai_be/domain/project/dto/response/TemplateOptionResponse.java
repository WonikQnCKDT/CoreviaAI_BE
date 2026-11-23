package org.wonik.coreviaai_be.domain.project.dto.response;

import java.util.List;

import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectTemplate;

/**
 * ② 템플릿 선택지와 기본값. FE는 템플릿을 고르면 아래 값으로 ②~④를 자동 세팅하고, 사용자는 생성 전에 개별로 바꿀 수 있다.
 */
public record TemplateOptionResponse(
	ProjectTemplate value,
	Methodology defaultMethodology,
	List<String> defaultPhases,
	List<ProjectActor> defaultActors,
	List<ProjectFeature> defaultFeatures
) {

	public static TemplateOptionResponse from(final ProjectTemplate template) {
		return new TemplateOptionResponse(
			template,
			template.getDefaultMethodology(),
			template.getDefaultPhases(),
			template.getDefaultActors().stream().sorted().toList(),
			template.getDefaultFeatures().stream().sorted().toList()
		);
	}
}
