package org.wonik.coreviaai_be.domain.project.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectStatus;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility;
import org.wonik.coreviaai_be.domain.project.service.result.ProjectDetailResult;

/**
 * 프로젝트 상세(FR-PRJ-01 인수 기준: 운영 방식을 프로젝트 정보에 표시). 운영 방식과 관계없는 공통 정보만 담는다.
 * 운영 방식별 구조는 별도 API로 조회한다(폭포수: GET /api/projects/{id}/phases). FE는 methodology로 어느 API를 부를지 고른다.
 *
 * @param actors 지정한 액터. 멤버 초대·역할 지정 화면의 역할 선택지다
 */
public record ProjectDetailResponse(
	Long id,
	String code,
	String name,
	ProjectStatus status,
	LocalDate startDate,
	LocalDate endDate,
	Long departmentId,
	String departmentName,
	ProjectVisibility visibility,
	Methodology methodology,
	DepartmentType departmentType,
	Long pmUserId,
	String pmName,
	Long createdBy,
	List<ProjectActor> actors,
	List<ProjectMemberResponse> members,
	Instant createdAt,
	Instant updatedAt
) {

	public static ProjectDetailResponse from(final ProjectDetailResult result) {
		final Project project = result.project();
		final Map<Long, String> userNames = result.userNames();
		return new ProjectDetailResponse(
			project.getId(),
			project.getCode(),
			project.getName(),
			project.getStatus(),
			project.getStartDate(),
			project.getEndDate(),
			project.getDepartmentId(),
			result.departmentName(),
			project.getVisibility(),
			project.getMethodology(),
			project.getDepartmentType(),
			project.getPmUserId(),
			userNames.get(project.getPmUserId()),
			project.getCreatedBy(),
			result.actors(),
			result.members().stream().map(member -> ProjectMemberResponse.of(member, userNames)).toList(),
			project.getCreatedAt(),
			project.getUpdatedAt()
		);
	}
}
