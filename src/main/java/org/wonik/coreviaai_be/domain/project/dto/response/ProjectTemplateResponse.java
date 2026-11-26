package org.wonik.coreviaai_be.domain.project.dto.response;

import java.util.List;

import org.wonik.coreviaai_be.domain.project.entity.ProjectTemplateDefinition;
import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectTemplate;
import org.wonik.coreviaai_be.domain.project.service.result.TemplateDefaults;

public record ProjectTemplateResponse(
	Long id,
	ProjectTemplate templateKey,
	String name,
	Methodology methodology,
	DepartmentType departmentType,
	List<String> phases,
	List<ProjectActor> actors,
	List<ProjectFeature> features,
	List<TemplateDefaults.RequiredDeliverable> requiredDeliverables,
	String status,
	long version
) {

	public static ProjectTemplateResponse from(final ProjectTemplateDefinition template, final TemplateDefaults defaults) {
		return new ProjectTemplateResponse(
			template.getId(),
			template.getTemplateKey(),
			template.getName(),
			template.getMethodology(),
			template.getDepartmentType(),
			defaults.phases(),
			defaults.actors(),
			defaults.features(),
			defaults.requiredDeliverables(),
			template.getStatus(),
			template.getVersion()
		);
	}
}
