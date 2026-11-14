package org.wonik.coreviaai_be.domain.admin.dto.request;

import org.wonik.global.common.type.SystemRole;

import jakarta.validation.constraints.NotNull;

public record ChangeSystemRoleRequest(
	@NotNull(message = "시스템 역할을 선택해 주세요")
	SystemRole systemRole
) {
}
