package org.wonik.authserver.domain.auth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.authserver.domain.auth.dto.request.ChangePasswordRequest;
import org.wonik.global.jwt.LoginUser;
import org.wonik.authserver.domain.auth.service.PasswordService;
import org.wonik.global.jwt.AuthCookieManager;
import org.wonik.global.common.response.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 본인 비밀번호 변경. 변경되면 모든 기기의 세션이 끊기고 새 비밀번호로 다시 로그인해야 한다.
 */
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class MyPasswordController {

	private final PasswordService passwordService;
	private final AuthCookieManager cookieManager;

	@PatchMapping("/password")
	public ApiResponse<Void> changePassword(
		@AuthenticationPrincipal final LoginUser loginUser,
		@Valid @RequestBody final ChangePasswordRequest changeRequest,
		final HttpServletResponse response
	) {
		passwordService.changeMyPassword(loginUser.userId(), changeRequest.currentPassword(), changeRequest.newPassword());
		cookieManager.expireTokens(response);
		return ApiResponse.success(null);
	}
}
