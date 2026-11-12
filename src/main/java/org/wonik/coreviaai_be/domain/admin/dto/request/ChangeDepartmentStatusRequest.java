package org.wonik.coreviaai_be.domain.admin.dto.request;

import org.wonik.global.department.entity.DepartmentStatus;

import jakarta.validation.constraints.NotNull;

public record ChangeDepartmentStatusRequest(
	@NotNull(message = "부서 상태를 선택해 주세요")
	DepartmentStatus status
) {
}
