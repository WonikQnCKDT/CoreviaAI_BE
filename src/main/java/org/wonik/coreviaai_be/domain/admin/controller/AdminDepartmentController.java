package org.wonik.coreviaai_be.domain.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.admin.dto.request.ChangeDepartmentStatusRequest;
import org.wonik.coreviaai_be.domain.admin.dto.request.CreateDepartmentRequest;
import org.wonik.coreviaai_be.domain.admin.dto.request.UpdateDepartmentRequest;
import org.wonik.coreviaai_be.domain.admin.dto.response.AdminDepartmentResponse;
import org.wonik.coreviaai_be.domain.admin.service.AdminDepartmentService;
import org.wonik.coreviaai_be.domain.department.entity.DepartmentStatus;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.common.response.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * 시스템 관리자의 부서(조직) 관리 API(FR-ADM-02). 접근 제어(SYSTEM_ADMIN)는 CoreviaSecurityConfig의 /api/admin/** 규칙이 맡는다.
 */
@RestController
@RequestMapping("/api/admin/departments")
@RequiredArgsConstructor
public class AdminDepartmentController {

	private final AdminDepartmentService adminDepartmentService;

	@GetMapping
	public ApiResponse<CursorPage<AdminDepartmentResponse>> findPage(
		@RequestParam(required = false) final String cursor,
		@RequestParam(defaultValue = "" + CursorPageRequest.DEFAULT_SIZE)
		@Min(1) @Max(CursorPageRequest.MAX_SIZE) final int size,
		@RequestParam(required = false) final DepartmentStatus status
	) {
		return ApiResponse.success(
			adminDepartmentService.findPage(CursorPageRequest.of(cursor, size), status).map(AdminDepartmentResponse::from)
		);
	}

	@PostMapping
	public ApiResponse<AdminDepartmentResponse> create(
		final AuditActor actor,
		@Valid @RequestBody final CreateDepartmentRequest request
	) {
		return ApiResponse.success(
			AdminDepartmentResponse.from(adminDepartmentService.create(actor, request.name(), request.parentId()))
		);
	}

	@PatchMapping("/{departmentId}")
	public ApiResponse<AdminDepartmentResponse> update(
		final AuditActor actor,
		@PathVariable @Positive final Long departmentId,
		@Valid @RequestBody final UpdateDepartmentRequest request
	) {
		return ApiResponse.success(AdminDepartmentResponse.from(
			adminDepartmentService.update(actor, departmentId, request.name(), request.parentId())
		));
	}

	@PatchMapping("/{departmentId}/status")
	public ApiResponse<AdminDepartmentResponse> changeStatus(
		final AuditActor actor,
		@PathVariable @Positive final Long departmentId,
		@Valid @RequestBody final ChangeDepartmentStatusRequest request
	) {
		return ApiResponse.success(
			AdminDepartmentResponse.from(adminDepartmentService.changeStatus(actor, departmentId, request.status()))
		);
	}
}
