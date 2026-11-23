package org.wonik.coreviaai_be.domain.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectPhaseRequest(
	@NotBlank(message = "단계 이름을 입력해 주세요")
	@Size(max = CreateProjectRequest.MAX_PHASE_NAME_LENGTH, message = "단계 이름은 100자 이하로 입력해 주세요")
	String name
) {
}
