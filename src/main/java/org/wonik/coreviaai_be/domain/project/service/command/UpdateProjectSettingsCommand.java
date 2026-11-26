package org.wonik.coreviaai_be.domain.project.service.command;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility;

/**
 * @param gateApproverPresent false면 승인자를 바꾸지 않는다. true이고 userId가 null이면 모든 단계에서 승인자를 비운다.
 */
public record UpdateProjectSettingsCommand(
	long version,
	BasicInfo basicInfo,
	Methodology methodology,
	Set<ProjectActor> actors,
	Set<ProjectFeature> features,
	Long gateApproverUserId,
	boolean gateApproverPresent
) {

	public record BasicInfo(
		LocalDate startDate,
		LocalDate endDate,
		List<Long> departmentIds,
		ProjectVisibility visibility
	) {
	}
}
