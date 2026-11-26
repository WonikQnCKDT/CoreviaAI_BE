package org.wonik.coreviaai_be.domain.admin.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.menu.dto.request.UpdateMenuTextRequest;
import org.wonik.coreviaai_be.domain.menu.dto.response.AdminMenuResponse;
import org.wonik.coreviaai_be.domain.menu.service.AdminMenuService;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.response.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * 시스템 관리자의 메뉴 문구 관리(FR-ADM-08). 접근 제어는 CoreviaSecurityConfig의 /api/admin/** 가 맡는다.
 * 메뉴 키 변경·삭제 API는 두지 않는다.
 */
@RestController
@RequestMapping("/api/admin/menus")
@RequiredArgsConstructor
public class AdminMenuController {

	private final AdminMenuService adminMenuService;

	@GetMapping
	public ApiResponse<List<AdminMenuResponse>> findAll() {
		return ApiResponse.success(adminMenuService.findAll().stream().map(AdminMenuResponse::from).toList());
	}

	@PatchMapping("/{menuId}")
	public ApiResponse<AdminMenuResponse> updateText(
		final AuditActor actor,
		@PathVariable @Positive final Long menuId,
		@Valid @RequestBody final UpdateMenuTextRequest request
	) {
		return ApiResponse.success(AdminMenuResponse.from(adminMenuService.updateText(
			actor, menuId, request.textKo(), request.textEn(), request.textZhTw(), request.version())));
	}
}
