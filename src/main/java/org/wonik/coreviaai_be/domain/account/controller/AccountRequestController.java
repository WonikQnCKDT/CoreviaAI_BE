package org.wonik.coreviaai_be.domain.account.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.account.dto.request.SubmitAccountRequest;
import org.wonik.coreviaai_be.domain.account.dto.response.AccountRequestStatusResponse;
import org.wonik.coreviaai_be.domain.account.dto.response.SubmittedAccountRequestResponse;
import org.wonik.coreviaai_be.domain.account.service.AccountRequestService;
import org.wonik.global.common.response.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * 로그인 전 계정 신청과 본인 상태 조회(FR-ADM-10). 승인·반려는 관리자 API가 맡는다.
 */
@RestController
@RequestMapping(AccountRequestController.PATH)
@RequiredArgsConstructor
public class AccountRequestController {

	public static final String PATH = "/api/account-requests";
	public static final String STATUS_PATH = PATH + "/status";

	private final AccountRequestService accountRequestService;

	@PostMapping
	public ApiResponse<SubmittedAccountRequestResponse> submit(
		final HttpServletRequest httpRequest,
		@Valid @RequestBody final SubmitAccountRequest request
	) {
		return ApiResponse.success(SubmittedAccountRequestResponse.from(
			accountRequestService.submit(httpRequest.getRemoteAddr(), request.toCommand())
		));
	}

	@GetMapping("/status")
	public ApiResponse<AccountRequestStatusResponse> status(
		@RequestParam @NotBlank @Size(max = 50) final String loginId,
		@RequestParam @NotBlank @Size(max = 50) final String name
	) {
		return ApiResponse.success(AccountRequestStatusResponse.from(
			accountRequestService.findOwnStatus(loginId, name)
		));
	}
}
