package org.wonik.coreviaai_be.domain.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.admin.dto.request.CreateLabelRequest;
import org.wonik.coreviaai_be.domain.admin.dto.request.UpdateLabelRequest;
import org.wonik.coreviaai_be.domain.admin.dto.response.AdminHistoryResponse;
import org.wonik.coreviaai_be.domain.admin.dto.response.AdminLabelResponse;
import org.wonik.coreviaai_be.domain.admin.service.AdminLabelService;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.common.response.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * 시스템 관리자의 다국어 라벨 관리 API(FR-ADM-06). 접근 제어(SYSTEM_ADMIN)는 CoreviaSecurityConfig의 /api/admin/** 규칙이 맡는다.
 * 라벨 키 변경·삭제 API는 두지 않는다(명세에 없음, 화면 코드가 키를 참조). 화면용 조회는 LabelController(/api/i18n/labels).
 */
@RestController
@RequestMapping("/api/admin/i18n/labels")
@RequiredArgsConstructor
public class AdminLabelController {

	private static final int MAX_KEYWORD_LENGTH = 100;
	private static final int MAX_CURSOR_LENGTH = 256;

	private final AdminLabelService adminLabelService;
	private final JsonMapper jsonMapper;

	/** missing=en|zh-TW면 그 언어 번역이 없는 라벨만 준다(인수 기준 "관리 화면에서 누락 필터 조회"). */
	@GetMapping
	public ApiResponse<CursorPage<AdminLabelResponse>> findPage(
		@RequestParam(required = false) final String cursor,
		@RequestParam(defaultValue = "" + CursorPageRequest.DEFAULT_SIZE)
		@Min(1) @Max(CursorPageRequest.MAX_SIZE) final int size,
		@RequestParam(required = false) @Size(max = MAX_KEYWORD_LENGTH) final String keyword,
		@RequestParam(required = false) @Size(max = MAX_KEYWORD_LENGTH) final String missing
	) {
		return ApiResponse.success(
			adminLabelService.findPage(CursorPageRequest.of(cursor, size), keyword, missing).map(AdminLabelResponse::from)
		);
	}

	@PostMapping
	public ApiResponse<AdminLabelResponse> create(
		final AuditActor actor,
		@Valid @RequestBody final CreateLabelRequest request
	) {
		return ApiResponse.success(AdminLabelResponse.from(adminLabelService.create(
			actor, request.labelKey(), request.textKo(), request.textEn(), request.textZhTw()
		)));
	}

	@PatchMapping("/{labelId}")
	public ApiResponse<AdminLabelResponse> update(
		final AuditActor actor,
		@PathVariable @Positive final Long labelId,
		@Valid @RequestBody final UpdateLabelRequest request
	) {
		return ApiResponse.success(AdminLabelResponse.from(adminLabelService.update(
			actor, labelId, request.textKo(), request.textEn(), request.textZhTw(), request.version()
		)));
	}

	@GetMapping("/{labelId}/histories")
	public ApiResponse<CursorPage<AdminHistoryResponse>> findHistories(
		@PathVariable @Positive final Long labelId,
		@RequestParam(required = false) @Size(max = MAX_CURSOR_LENGTH) final String cursor,
		@RequestParam(defaultValue = "" + CursorPageRequest.DEFAULT_SIZE)
		@Min(1) @Max(CursorPageRequest.MAX_SIZE) final int size
	) {
		return ApiResponse.success(
			adminLabelService.findHistories(labelId, CursorPageRequest.of(cursor, size))
				.map(result -> AdminHistoryResponse.from(result, jsonMapper))
		);
	}
}
