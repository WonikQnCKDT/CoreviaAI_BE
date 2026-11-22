package org.wonik.coreviaai_be.domain.admin.controller;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.admin.dto.response.AdminErrorLogDetailResponse;
import org.wonik.coreviaai_be.domain.admin.dto.response.AdminErrorLogResponse;
import org.wonik.coreviaai_be.domain.admin.service.AdminErrorLogService;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.common.response.ApiResponse;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

/**
 * 시스템 관리자의 에러 로그 조회 API(FR-ADM-09). 접근 제어(SYSTEM_ADMIN)는 CoreviaSecurityConfig의 /api/admin/** 규칙이 맡는다.
 * 에러 로그는 수정·삭제 API를 두지 않는다(append-only). 기간은 ISO-8601 UTC 시각(예: 2026-11-01T00:00:00Z), from 포함·to 제외.
 */
@RestController
@RequestMapping("/api/admin/error-logs")
@RequiredArgsConstructor
public class AdminErrorLogController {

	private static final int MAX_ERROR_TYPE_LENGTH = 200;
	private static final int MAX_API_PATH_LENGTH = 300;

	private final AdminErrorLogService adminErrorLogService;

	@GetMapping
	public ApiResponse<CursorPage<AdminErrorLogResponse>> findPage(
		@RequestParam(required = false) final String cursor,
		@RequestParam(defaultValue = "" + CursorPageRequest.DEFAULT_SIZE)
		@Min(1) @Max(CursorPageRequest.MAX_SIZE) final int size,
		@RequestParam(required = false) final Instant from,
		@RequestParam(required = false) final Instant to,
		@RequestParam(required = false) @Size(max = MAX_ERROR_TYPE_LENGTH) final String errorType,
		@RequestParam(required = false) @Size(max = MAX_API_PATH_LENGTH) final String apiPath
	) {
		return ApiResponse.success(adminErrorLogService
			.findPage(CursorPageRequest.of(cursor, size), from, to, errorType, apiPath)
			.map(AdminErrorLogResponse::from));
	}

	@GetMapping("/{errorLogId}")
	public ApiResponse<AdminErrorLogDetailResponse> find(@PathVariable @Positive final Long errorLogId) {
		return ApiResponse.success(AdminErrorLogDetailResponse.from(adminErrorLogService.find(errorLogId)));
	}
}
