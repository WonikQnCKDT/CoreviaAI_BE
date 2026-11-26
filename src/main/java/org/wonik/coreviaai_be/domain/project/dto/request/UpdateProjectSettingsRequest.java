package org.wonik.coreviaai_be.domain.project.dto.request;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility;
import org.wonik.coreviaai_be.domain.project.service.command.UpdateProjectSettingsCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 생성 후 프로젝트 설정 변경(FR-PRJ-14). 넣은 구역만 바꾼다. 게이트 승인자를 비우려면 gateApprover.userId를 null로 보낸다.
 */
public record UpdateProjectSettingsRequest(
	@NotNull(message = "프로젝트 버전을 확인해 주세요")
	Long version,

	@Valid BasicInfo basicInfo,
	Methodology methodology,
	Set<@NotNull(message = "액터 값을 확인해 주세요") ProjectActor> actors,
	Set<@NotNull(message = "기능 모듈 값을 확인해 주세요") ProjectFeature> features,
	@Valid GateApprover gateApprover
) {

	public UpdateProjectSettingsCommand toCommand() {
		return new UpdateProjectSettingsCommand(
			version,
			basicInfo == null ? null : new UpdateProjectSettingsCommand.BasicInfo(
				basicInfo.startDate(),
				basicInfo.endDate(),
				List.copyOf(basicInfo.departmentIds()),
				basicInfo.visibility()
			),
			methodology,
			actors == null ? null : Set.copyOf(actors),
			features == null ? null : Set.copyOf(features),
			gateApprover == null ? null : gateApprover.userId(),
			gateApprover != null
		);
	}

	public record BasicInfo(
		LocalDate startDate,
		LocalDate endDate,
		@NotNull(message = "주관 부서를 확인해 주세요")
		@Size(max = CreateProjectRequest.MAX_DEPARTMENTS, message = "주관 부서는 최대 20개까지 지정할 수 있습니다")
		List<@NotNull(message = "주관 부서 ID를 확인해 주세요") Long> departmentIds,
		@NotNull(message = "공개 범위를 선택해 주세요")
		ProjectVisibility visibility
	) {
	}

	/** userId가 null이면 모든 단계의 게이트 승인자를 비운다. */
	public record GateApprover(Long userId) {
	}
}
