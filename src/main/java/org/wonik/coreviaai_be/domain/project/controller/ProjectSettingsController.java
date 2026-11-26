package org.wonik.coreviaai_be.domain.project.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.project.dto.request.UpdateProjectSettingsRequest;
import org.wonik.coreviaai_be.domain.project.dto.response.ProjectDetailResponse;
import org.wonik.coreviaai_be.domain.project.dto.response.ProjectSettingHistoryResponse;
import org.wonik.coreviaai_be.domain.project.service.ProjectQueryService;
import org.wonik.coreviaai_be.domain.project.service.ProjectSettingsService;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.jwt.LoginUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * 생성 후 프로젝트 설정 변경(FR-PRJ-14). 변경은 PM만, 이력 조회는 프로젝트를 볼 수 있는 사용자다.
 */
@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class ProjectSettingsController {

	private final ProjectSettingsService settingsService;
	private final ProjectQueryService queryService;

	@PatchMapping("/settings")
	public ApiResponse<ProjectDetailResponse> update(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@Valid @RequestBody final UpdateProjectSettingsRequest request
	) {
		settingsService.update(actor, projectId, request.toCommand());
		return ApiResponse.success(ProjectDetailResponse.from(queryService.getDetail(actor.userId(), projectId)));
	}

	@GetMapping("/setting-histories")
	public ApiResponse<CursorPage<ProjectSettingHistoryResponse>> findHistories(
		@AuthenticationPrincipal final LoginUser loginUser,
		@PathVariable @Positive final Long projectId,
		@RequestParam(required = false) final String cursor,
		@RequestParam(defaultValue = "" + CursorPageRequest.DEFAULT_SIZE)
		@Min(1) @Max(CursorPageRequest.MAX_SIZE) final int size
	) {
		final CursorPageRequest page = CursorPageRequest.of(cursor, size);
		return ApiResponse.success(
			settingsService.findHistories(loginUser.userId(), projectId, page).map(ProjectSettingHistoryResponse::from)
		);
	}
}
