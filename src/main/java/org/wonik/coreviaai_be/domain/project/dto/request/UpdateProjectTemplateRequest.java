package org.wonik.coreviaai_be.domain.project.dto.request;

import java.util.List;
import java.util.Set;

import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectTemplate;
import org.wonik.coreviaai_be.domain.project.service.command.UpdateProjectTemplateCommand;
import org.wonik.coreviaai_be.domain.project.service.result.TemplateDefaults.RequiredDeliverable;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 템플릿 기본값 수정(FR-PRJ-09). 시스템 관리자만 호출한다. 이미 만든 프로젝트에는 적용되지 않는다.
 */
public record UpdateProjectTemplateRequest(
	@NotNull(message = "템플릿 버전을 확인해 주세요")
	Long version,

	@NotBlank(message = "템플릿 이름을 입력해 주세요")
	@Size(max = 100, message = "템플릿 이름은 100자 이하로 입력해 주세요")
	String name,

	@NotNull(message = "운영 방식을 선택해 주세요")
	Methodology methodology,

	DepartmentType departmentType,

	@NotNull(message = "기본 단계를 확인해 주세요")
	@Size(max = CreateProjectRequest.MAX_PHASES, message = "프로젝트 단계는 최대 20개까지 지정할 수 있습니다")
	List<@NotBlank(message = "단계 이름을 입력해 주세요") @Size(max = CreateProjectRequest.MAX_PHASE_NAME_LENGTH,
		message = "단계 이름은 100자 이하로 입력해 주세요") String> phases,

	@NotEmpty(message = "액터를 지정해 주세요")
	Set<@NotNull(message = "액터 값을 확인해 주세요") ProjectActor> actors,

	@NotNull(message = "기능 구성을 선택해 주세요")
	Set<@NotNull(message = "기능 모듈 값을 확인해 주세요") ProjectFeature> features,

	@NotNull(message = "필수 산출물을 확인해 주세요")
	@Size(max = 100, message = "필수 산출물은 최대 100개까지 지정할 수 있습니다")
	List<@NotNull(message = "필수 산출물 값을 확인해 주세요") @Valid RequiredDeliverableRequest> requiredDeliverables
) {

	public UpdateProjectTemplateCommand toCommand(final ProjectTemplate templateKey) {
		return new UpdateProjectTemplateCommand(
			templateKey,
			version,
			name,
			methodology,
			departmentType,
			List.copyOf(phases),
			Set.copyOf(actors),
			Set.copyOf(features),
			requiredDeliverables.stream()
				.map(item -> new RequiredDeliverable(item.phaseName(), item.name()))
				.toList()
		);
	}

	public record RequiredDeliverableRequest(
		@NotBlank(message = "산출물이 속한 단계 이름을 입력해 주세요")
		@Size(max = CreateProjectRequest.MAX_PHASE_NAME_LENGTH, message = "단계 이름은 100자 이하로 입력해 주세요")
		String phaseName,

		@NotBlank(message = "산출물 이름을 입력해 주세요")
		@Size(max = CreateProjectRequest.MAX_PHASE_NAME_LENGTH, message = "산출물 이름은 100자 이하로 입력해 주세요")
		String name
	) {
	}
}
