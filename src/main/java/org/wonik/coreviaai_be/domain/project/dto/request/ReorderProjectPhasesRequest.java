package org.wonik.coreviaai_be.domain.project.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ReorderProjectPhasesRequest(
	@NotEmpty(message = "단계 순서를 입력해 주세요")
	@Size(max = CreateProjectRequest.MAX_PHASES, message = "프로젝트 단계는 최대 20개까지 지정할 수 있습니다")
	List<@NotNull(message = "단계 ID를 확인해 주세요") @Positive(message = "단계 ID를 확인해 주세요") Long> phaseIds
) {
}
