package org.wonik.coreviaai_be.domain.project.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectStatus;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectTemplate;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility;
import org.wonik.coreviaai_be.domain.project.service.result.ProjectDetailResult;

/**
 * 프로젝트 상세(FR-PRJ-01 인수 기준: 운영 방식을 프로젝트 정보에 표시). 운영 방식과 관계없는 공통 정보만 담는다.
 * 운영 방식별 구조는 별도 API로 조회한다(폭포수: GET /api/projects/{id}/phases). FE는 methodology로 어느 API를 부를지 고른다.
 * 멤버는 커서 목록 API(GET /api/projects/{id}/members)로 조회한다.
 *
 * @param departments 주관 부서(0~N개)
 * @param pms         PM(1명 이상, 등록순)
 * @param actors      지정한 액터. 멤버 초대·역할 지정 화면의 역할 선택지다
 * @param features    사용하는 기능 모듈(FR-PRJ-06). 없는 모듈은 프로젝트 메뉴에 표시하지 않는다
 */
public record ProjectDetailResponse(
	Long id,
	String code,
	String name,
	ProjectStatus status,
	LocalDate startDate,
	LocalDate endDate,
	List<NamedRefResponse> departments,
	ProjectVisibility visibility,
	ProjectTemplate template,
	Methodology methodology,
	DepartmentType departmentType,
	List<NamedRefResponse> pms,
	Long createdBy,
	List<ProjectActor> actors,
	List<ProjectFeature> features,
	int progress,
	Instant createdAt,
	Instant updatedAt,
	long version
) {

	public static ProjectDetailResponse from(final ProjectDetailResult result) {
		final Project project = result.project();
		return new ProjectDetailResponse(
			project.getId(),
			project.getCode(),
			project.getName(),
			project.getStatus(),
			project.getStartDate(),
			project.getEndDate(),
			NamedRefResponse.listOf(result.departments()),
			project.getVisibility(),
			project.getTemplate(),
			project.getMethodology(),
			project.getDepartmentType(),
			NamedRefResponse.listOf(result.pms()),
			project.getCreatedBy(),
			result.actors(),
			result.features(),
			result.progress(),
			project.getCreatedAt(),
			project.getUpdatedAt(),
			project.getVersion()
		);
	}
}
