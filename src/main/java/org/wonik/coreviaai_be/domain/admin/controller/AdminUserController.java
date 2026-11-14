package org.wonik.coreviaai_be.domain.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.admin.dto.request.ChangeProjectCreationRequest;
import org.wonik.coreviaai_be.domain.admin.dto.request.ChangeSystemRoleRequest;
import org.wonik.coreviaai_be.domain.admin.dto.request.ChangeUserStatusRequest;
import org.wonik.coreviaai_be.domain.admin.dto.request.CreateUserRequest;
import org.wonik.coreviaai_be.domain.admin.dto.request.UpdateUserProfileRequest;
import org.wonik.coreviaai_be.domain.admin.dto.response.AdminUserResponse;
import org.wonik.coreviaai_be.domain.admin.dto.response.UserHistoryResponse;
import org.wonik.coreviaai_be.domain.admin.service.AdminUserService;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.UserStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * 시스템 관리자의 사용자 계정 관리 API(FR-ADM-02). 접근 제어(SYSTEM_ADMIN)는 CoreviaSecurityConfig의 /api/admin/** 규칙이 맡는다.
 * 비밀번호 발급·초기화(/api/admin/users/{userId}/password-reset)는 authserver가 맡는다.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

	private final AdminUserService adminUserService;
	private final JsonMapper jsonMapper;

	@GetMapping
	public ApiResponse<CursorPage<AdminUserResponse>> search(
		@RequestParam(required = false) final String cursor,
		@RequestParam(defaultValue = "" + CursorPageRequest.DEFAULT_SIZE)
		@Min(1) @Max(CursorPageRequest.MAX_SIZE) final int size,
		@RequestParam(required = false) @Positive final Long departmentId,
		@RequestParam(required = false) final UserStatus status,
		@RequestParam(required = false) final SystemRole systemRole,
		@RequestParam(required = false) @Size(max = 50) final String keyword
	) {
		final CursorPageRequest page = CursorPageRequest.of(cursor, size);
		return ApiResponse.success(
			adminUserService.search(page, departmentId, status, systemRole, keyword).map(AdminUserResponse::from)
		);
	}

	@GetMapping("/{userId}")
	public ApiResponse<AdminUserResponse> get(@PathVariable @Positive final Long userId) {
		return ApiResponse.success(AdminUserResponse.from(adminUserService.get(userId)));
	}

	@PostMapping
	public ApiResponse<AdminUserResponse> create(
		final AuditActor actor,
		@Valid @RequestBody final CreateUserRequest request
	) {
		return ApiResponse.success(AdminUserResponse.from(adminUserService.create(actor, request.toCommand())));
	}

	@PatchMapping("/{userId}")
	public ApiResponse<AdminUserResponse> updateProfile(
		final AuditActor actor,
		@PathVariable @Positive final Long userId,
		@Valid @RequestBody final UpdateUserProfileRequest request
	) {
		return ApiResponse.success(
			AdminUserResponse.from(adminUserService.updateProfile(actor, userId, request.toCommand()))
		);
	}

	@PatchMapping("/{userId}/system-role")
	public ApiResponse<AdminUserResponse> changeSystemRole(
		final AuditActor actor,
		@PathVariable @Positive final Long userId,
		@Valid @RequestBody final ChangeSystemRoleRequest request
	) {
		return ApiResponse.success(
			AdminUserResponse.from(adminUserService.changeSystemRole(actor, userId, request.systemRole()))
		);
	}

	@PatchMapping("/{userId}/status")
	public ApiResponse<AdminUserResponse> changeStatus(
		final AuditActor actor,
		@PathVariable @Positive final Long userId,
		@Valid @RequestBody final ChangeUserStatusRequest request
	) {
		return ApiResponse.success(AdminUserResponse.from(adminUserService.changeStatus(actor, userId, request.status())));
	}

	@PatchMapping("/{userId}/project-creation")
	public ApiResponse<AdminUserResponse> changeProjectCreation(
		final AuditActor actor,
		@PathVariable @Positive final Long userId,
		@Valid @RequestBody final ChangeProjectCreationRequest request
	) {
		return ApiResponse.success(
			AdminUserResponse.from(adminUserService.changeProjectCreation(actor, userId, request.enabled()))
		);
	}

	@GetMapping("/{userId}/histories")
	public ApiResponse<CursorPage<UserHistoryResponse>> histories(
		@PathVariable @Positive final Long userId,
		@RequestParam(required = false) final String cursor,
		@RequestParam(defaultValue = "" + CursorPageRequest.DEFAULT_SIZE)
		@Min(1) @Max(CursorPageRequest.MAX_SIZE) final int size
	) {
		return ApiResponse.success(
			adminUserService.findHistories(userId, CursorPageRequest.of(cursor, size))
				.map(result -> UserHistoryResponse.from(result, jsonMapper))
		);
	}
}
