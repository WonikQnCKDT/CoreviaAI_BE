package org.wonik.authserver.domain.auth.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.authserver.domain.auth.dto.response.PasswordResetResponse;
import org.wonik.global.jwt.LoginUser;
import org.wonik.authserver.domain.auth.service.PasswordService;
import org.wonik.global.common.response.ApiResponse;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * 시스템 관리자의 비밀번호 초기화(잠금 해제 포함). /api/admin/** 는 SecurityConfig에서 SYSTEM_ADMIN만 허용한다.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminPasswordController {

	private final PasswordService passwordService;

	@PostMapping("/{userId}/password-reset")
	public ResponseEntity<ApiResponse<PasswordResetResponse>> resetPassword(
		@AuthenticationPrincipal final LoginUser admin,
		@PathVariable @Positive final Long userId
	) {
		final String temporaryPassword = passwordService.resetPassword(admin.userId(), userId);
		return ResponseEntity.ok()
			.cacheControl(CacheControl.noStore())
			.body(ApiResponse.success(new PasswordResetResponse(temporaryPassword)));
	}
}
