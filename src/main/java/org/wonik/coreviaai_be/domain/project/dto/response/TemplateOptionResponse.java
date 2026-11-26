package org.wonik.coreviaai_be.domain.project.dto.response;

import java.util.List;

import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.ProjectTemplateDefinition;
import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectTemplate;
import org.wonik.coreviaai_be.domain.project.service.result.TemplateDefaults;

/**
 * ② 템플릿 선택지와 기본값. FE는 템플릿을 고르면 아래 값으로 ②~④를 자동 세팅하고, 사용자는 생성 전에 개별로 바꿀 수 있다.
 */
public record TemplateOptionResponse(
	ProjectTemplate value,
	String name,
	Methodology defaultMethodology,
	DepartmentType defaultDepartmentType,
	List<String> defaultPhases,
	List<ProjectActor> defaultActors,
	List<ProjectFeature> defaultFeatures,
	List<TemplateDefaults.RequiredDeliverable> requiredDeliverables
) {

	public static TemplateOptionResponse from(
		final ProjectTemplateDefinition template,
		final TemplateDefaults defaults
	) {
		return new TemplateOptionResponse(
			template.getTemplateKey(),
			template.getName(),
			template.getMethodology(),
			template.getDepartmentType(),
			defaults.phases(),
			defaults.actors(),
			defaults.features(),
			defaults.requiredDeliverables()
		);
	}
}
