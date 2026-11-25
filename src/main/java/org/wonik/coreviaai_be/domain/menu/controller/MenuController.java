package org.wonik.coreviaai_be.domain.menu.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.menu.dto.response.MenuItemResponse;
import org.wonik.coreviaai_be.domain.menu.service.MenuQueryService;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.jwt.LoginUser;

import lombok.RequiredArgsConstructor;

/**
 * 로그인 사용자용 메뉴 조회(FR-ADM-08). 관리자 수정은 AdminMenuController.
 */
@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

	private final MenuQueryService menuQueryService;

	@GetMapping
	public ResponseEntity<ApiResponse<List<MenuItemResponse>>> findVisible(
		@AuthenticationPrincipal final LoginUser viewer,
		@RequestParam(required = false) final String lang,
		@RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) final String acceptLanguage
	) {
		final var menus = menuQueryService.findVisible(viewer.userId(), lang, acceptLanguage).stream()
			.map(MenuItemResponse::from)
			.toList();
		return ResponseEntity.ok()
			.header(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE)
			.body(ApiResponse.success(menus));
	}
}
