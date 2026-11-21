package org.wonik.coreviaai_be.domain.project.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectStatus;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectTemplate;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility;
import org.wonik.coreviaai_be.domain.project.service.result.ProjectSummaryResult;

/**
 * 프로젝트 목록 항목. 리스크 등급은 FR-AIR-06 구현 후 추가한다.
 *
 * @param departments 주관 부서(0~N개)
 * @param pms         PM(1명 이상)
 * @param startDate   시작일. 아직 입력하지 않았으면 null
 * @param endDate     종료일. 아직 입력하지 않았으면 null
 */
public record ProjectSummaryResponse(
	Long id,
	String code,
	String name,
	ProjectStatus status,
	ProjectTemplate template,
	Methodology methodology,
	ProjectVisibility visibility,
	List<NamedRefResponse> departments,
	List<NamedRefResponse> pms,
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
			project.getTemplate(),
			project.getMethodology(),
			project.getVisibility(),
			NamedRefResponse.listOf(result.departments()),
			NamedRefResponse.listOf(result.pms()),
			project.getStartDate(),
			project.getEndDate(),
			project.getCreatedAt()
		);
	}
}
