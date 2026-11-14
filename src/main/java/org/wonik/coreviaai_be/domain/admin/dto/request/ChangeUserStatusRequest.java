package org.wonik.coreviaai_be.domain.admin.dto.request;

import org.wonik.global.user.entity.UserStatus;

import jakarta.validation.constraints.NotNull;

public record ChangeUserStatusRequest(
	@NotNull(message = "계정 상태를 선택해 주세요")
	UserStatus status
) {
}
