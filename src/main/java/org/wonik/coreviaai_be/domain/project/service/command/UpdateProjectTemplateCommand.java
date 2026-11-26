package org.wonik.coreviaai_be.domain.project.service.command;

import java.util.List;
import java.util.Set;

import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectTemplate;
import org.wonik.coreviaai_be.domain.project.service.result.TemplateDefaults.RequiredDeliverable;

public record UpdateProjectTemplateCommand(
	ProjectTemplate templateKey,
	long version,
	String name,
	Methodology methodology,
	DepartmentType departmentType,
	List<String> phases,
	Set<ProjectActor> actors,
	Set<ProjectFeature> features,
	List<RequiredDeliverable> requiredDeliverables
) {
}
