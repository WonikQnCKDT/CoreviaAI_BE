package org.wonik.coreviaai_be.domain.account.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.account.dto.request.ApproveAccountRequest;
import org.wonik.coreviaai_be.domain.account.dto.request.RejectAccountRequest;
import org.wonik.coreviaai_be.domain.account.dto.response.AccountRequestResponse;
import org.wonik.coreviaai_be.domain.account.entity.AccountRequestStatus;
import org.wonik.coreviaai_be.domain.account.service.AccountRequestService;
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
 * 계정 신청 승인·반려(FR-ADM-10). 접근 제어는 /api/admin/** 규칙이 맡는다.
 */
@RestController
@RequestMapping("/api/admin/account-requests")
@RequiredArgsConstructor
public class AdminAccountRequestController {

	private final AccountRequestService accountRequestService;

	@GetMapping
	public ApiResponse<CursorPage<AccountRequestResponse>> search(
		@RequestParam(required = false) final String cursor,
		@RequestParam(defaultValue = "" + CursorPageRequest.DEFAULT_SIZE)
		@Min(1) @Max(CursorPageRequest.MAX_SIZE) final int size,
		@RequestParam(required = false) final AccountRequestStatus status
	) {
		final CursorPageRequest page = CursorPageRequest.of(cursor, size);
		return ApiResponse.success(
			accountRequestService.search(page, status).map(AccountRequestResponse::from)
		);
	}

	@PostMapping("/{requestId}/approve")
	public ApiResponse<AccountRequestResponse> approve(
		final AuditActor actor,
		@PathVariable @Positive final Long requestId,
		@Valid @RequestBody final ApproveAccountRequest request
	) {
		return ApiResponse.success(AccountRequestResponse.from(
			accountRequestService.approve(actor, requestId, request.toCommand())
		));
	}

	@PostMapping("/{requestId}/reject")
	public ApiResponse<AccountRequestResponse> reject(
		final AuditActor actor,
		@PathVariable @Positive final Long requestId,
		@Valid @RequestBody final RejectAccountRequest request
	) {
		return ApiResponse.success(AccountRequestResponse.from(
			accountRequestService.reject(actor, requestId, request.toCommand())
		));
	}
}
