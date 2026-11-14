package org.wonik.coreviaai_be.domain.project.dto.response;

import java.time.Instant;
import java.time.LocalDate;

import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectStatus;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility;
import org.wonik.coreviaai_be.domain.project.service.result.ProjectSummaryResult;

/**
 * 프로젝트 목록 항목. 리스크 등급은 FR-AIR-06 구현 후 추가한다.
 */
public record ProjectSummaryResponse(
	Long id,
	String code,
	String name,
	ProjectStatus status,
	Methodology methodology,
	ProjectVisibility visibility,
	Long departmentId,
	String departmentName,
	Long pmUserId,
	String pmName,
	LocalDate startDate,
	LocalDate endDate,
	Instant createdAt
) {

	public static ProjectSummaryResponse from(final ProjectSummaryResult result) {
		final Project project = result.project();
		return new ProjectSummaryResponse(
			project.getId(),
			project.getCode(),
			project.getName(),
			project.getStatus(),
			project.getMethodology(),
			project.getVisibility(),
			project.getDepartmentId(),
			result.departmentName(),
			project.getPmUserId(),
			result.pmName(),
			project.getStartDate(),
			project.getEndDate(),
			project.getCreatedAt()
		);
	}
}
