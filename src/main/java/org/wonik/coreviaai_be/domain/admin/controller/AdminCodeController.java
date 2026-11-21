package org.wonik.coreviaai_be.domain.admin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.admin.dto.request.CreateCodeRequest;
import org.wonik.coreviaai_be.domain.admin.dto.request.UpdateCodeRequest;
import org.wonik.coreviaai_be.domain.admin.dto.response.AdminCodeGroupResponse;
import org.wonik.coreviaai_be.domain.admin.dto.response.AdminCodeResponse;
import org.wonik.coreviaai_be.domain.admin.service.AdminCodeService;
import org.wonik.coreviaai_be.domain.code.entity.CommonCode;
import org.wonik.coreviaai_be.domain.code.service.result.GroupCodes;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.response.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

/**
 * 시스템 관리자의 공통 코드 관리 API(FR-ADM-07). 접근 제어(SYSTEM_ADMIN)는 CoreviaSecurityConfig의 /api/admin/** 규칙이 맡는다.
 * 코드 값 수정·삭제 API는 두지 않는다(인수 기준: 코드 값 수정·삭제 불가). 그런 요청은 매핑이 없어 404로 끝난다.
 */
@RestController
@RequestMapping("/api/admin/code-groups")
@RequiredArgsConstructor
public class AdminCodeController {

	private final AdminCodeService adminCodeService;

	@GetMapping
	public ApiResponse<List<AdminCodeGroupResponse>> findGroups() {
		return ApiResponse.success(adminCodeService.findGroups().stream().map(AdminCodeGroupResponse::from).toList());
	}

	@GetMapping("/{groupCode}/codes")
	public ApiResponse<List<AdminCodeResponse>> findCodes(
		@PathVariable @Pattern(regexp = CommonCode.GROUP_CODE_PATTERN) final String groupCode
	) {
		final GroupCodes result = adminCodeService.findCodes(groupCode);
		return ApiResponse.success(result.codes().stream()
			.map(code -> AdminCodeResponse.from(result.group().getGroupCode(), code))
			.toList());
	}

	@PostMapping("/{groupCode}/codes")
	public ApiResponse<AdminCodeResponse> create(
		final AuditActor actor,
		@PathVariable @Pattern(regexp = CommonCode.GROUP_CODE_PATTERN) final String groupCode,
		@Valid @RequestBody final CreateCodeRequest request
	) {
		return ApiResponse.success(AdminCodeResponse.from(
			groupCode,
			adminCodeService.create(actor, groupCode, request.code(), request.sortOrder())
		));
	}

	@PatchMapping("/{groupCode}/codes/{code}")
	public ApiResponse<AdminCodeResponse> update(
		final AuditActor actor,
		@PathVariable @Pattern(regexp = CommonCode.GROUP_CODE_PATTERN) final String groupCode,
		@PathVariable @Pattern(regexp = CommonCode.CODE_PATTERN) final String code,
		@Valid @RequestBody final UpdateCodeRequest request
	) {
		return ApiResponse.success(AdminCodeResponse.from(
			groupCode,
			adminCodeService.update(actor, groupCode, code, request.sortOrder(), request.active())
		));
	}
}
