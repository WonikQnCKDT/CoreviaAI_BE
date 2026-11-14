package org.wonik.authserver.domain.auth.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.authserver.domain.auth.dto.request.LoginRequest;
import org.wonik.authserver.domain.auth.dto.response.AccountResponse;
import org.wonik.global.jwt.LoginUser;
import org.wonik.authserver.domain.auth.service.AuthService;
import org.wonik.authserver.domain.auth.service.IssuedLogin;
import org.wonik.global.jwt.AuthCookieManager;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 로그인·로그아웃·토큰 재발급·내 계정 조회(FR-ADM-01). 토큰은 HttpOnly 쿠키로만 주고받는다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final AuthCookieManager cookieManager;

	@PostMapping("/login")
	public ApiResponse<AccountResponse> login(
		@Valid @RequestBody final LoginRequest loginRequest,
		final HttpServletRequest request,
		final HttpServletResponse response
	) {
		final IssuedLogin issued = authService.login(
			loginRequest.loginId(),
			loginRequest.password(),
			request.getRemoteAddr()
		);
		cookieManager.writeTokens(response, issued.accessToken(), issued.refreshToken());
		return ApiResponse.success(AccountResponse.from(issued.account()));
	}

	@PostMapping("/refresh")
	public ApiResponse<Void> refresh(final HttpServletRequest request, final HttpServletResponse response) {
		final String refreshToken = cookieManager.readRefreshToken(request)
			.orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
		cookieManager.writeAccessToken(response, authService.refresh(refreshToken));
		return ApiResponse.success(null);
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(final HttpServletRequest request, final HttpServletResponse response) {
		authService.logout(cookieManager.readAccessToken(request), cookieManager.readRefreshToken(request));
		cookieManager.expireTokens(response);
		return ApiResponse.success(null);
	}

	@GetMapping("/me")
	public ApiResponse<AccountResponse> me(@AuthenticationPrincipal final LoginUser loginUser) {
		return ApiResponse.success(authService.getAccount(loginUser.userId()));
	}
}
