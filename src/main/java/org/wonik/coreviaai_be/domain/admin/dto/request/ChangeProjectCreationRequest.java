package org.wonik.coreviaai_be.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 프로젝트 생성 권한 부여(true)·회수(false).
 */
public record ChangeProjectCreationRequest(
	@NotNull(message = "프로젝트 생성 권한 부여 여부를 선택해 주세요")
	Boolean enabled
) {
}
