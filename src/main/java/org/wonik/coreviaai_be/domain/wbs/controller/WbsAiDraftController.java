package org.wonik.coreviaai_be.domain.wbs.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.wbs.dto.request.ApplyWbsDraftRequest;
import org.wonik.coreviaai_be.domain.wbs.dto.request.GenerateWbsDraftRequest;
import org.wonik.coreviaai_be.domain.wbs.dto.response.ApplyWbsDraftResponse;
import org.wonik.coreviaai_be.domain.wbs.dto.response.WbsDraftResponse;
import org.wonik.coreviaai_be.domain.wbs.service.WbsDraftApplyService;
import org.wonik.coreviaai_be.domain.wbs.service.WbsDraftGenerationService;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.response.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * AI WBS 초안 API(FR-PRJ-07). 생성은 저장 없이 초안만 돌려주고, PM이 승인한 항목만 반영 API로 WBS에 등록한다.
 * 둘 다 현재 PM만 호출할 수 있다(볼 수 없으면 404, PM이 아니면 403 PROJECT_PM_ONLY). 판정은 서비스가 한다.
 * /api/projects/** 경로라 시스템 관리자는 보안 설정에서 이미 차단된다.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/wbs/ai-draft")
@RequiredArgsConstructor
public class WbsAiDraftController {

	private final WbsDraftGenerationService generationService;
	private final WbsDraftApplyService applyService;

	@PostMapping
	public ApiResponse<WbsDraftResponse> generate(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@Valid @RequestBody final GenerateWbsDraftRequest request
	) {
		return ApiResponse.success(WbsDraftResponse.from(generationService.generate(actor, projectId, request.toCommand())));
	}

	/** 승인 항목만 반영한다. 같은 초안 토큰으로 다시 보내면 409 AI_DRAFT_ALREADY_APPLIED로 끝난다. */
	@PostMapping("/apply")
	public ApiResponse<ApplyWbsDraftResponse> apply(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@Valid @RequestBody final ApplyWbsDraftRequest request
	) {
		return ApiResponse.success(ApplyWbsDraftResponse.from(applyService.apply(actor, projectId, request.toCommand())));
	}
}
