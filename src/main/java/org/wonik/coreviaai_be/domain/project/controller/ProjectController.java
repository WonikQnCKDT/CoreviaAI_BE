package org.wonik.coreviaai_be.domain.project.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.project.dto.request.CreateProjectRequest;
import org.wonik.coreviaai_be.domain.project.dto.response.CodeAvailabilityResponse;
import org.wonik.coreviaai_be.domain.project.dto.response.CreationOptionsResponse;
import org.wonik.coreviaai_be.domain.project.dto.response.ProjectDetailResponse;
import org.wonik.coreviaai_be.domain.project.dto.response.ProjectPhaseResponse;
import org.wonik.coreviaai_be.domain.project.dto.response.ProjectSummaryResponse;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectStatus;
import org.wonik.coreviaai_be.domain.project.service.ProjectCreationService;
import org.wonik.coreviaai_be.domain.project.service.ProjectQueryService;
import org.wonik.coreviaai_be.domain.project.service.result.ProjectPhasesResult;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.jwt.LoginUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 생성·목록·상세 API(FR-PRJ-01). 멤버·PM 관리는 ProjectMemberController(FR-PRJ-04). 일반 사용자·경영진만 접근한다(CoreviaSecurityConfig, 시스템 관리자 차단).
 * 생성 권한(프로젝트 생성 권한 보유)과 공개 범위 판정은 서비스가 한다.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

	private final ProjectCreationService projectCreationService;
	private final ProjectQueryService projectQueryService;

	@GetMapping
	public ApiResponse<CursorPage<ProjectSummaryResponse>> findPage(
		@AuthenticationPrincipal final LoginUser loginUser,
		@RequestParam(required = false) final String cursor,
		@RequestParam(defaultValue = "" + CursorPageRequest.DEFAULT_SIZE)
		@Min(1) @Max(CursorPageRequest.MAX_SIZE) final int size,
		@RequestParam(required = false) final ProjectStatus status,
		@RequestParam(required = false) @Positive final Long departmentId,
		@RequestParam(required = false) @Positive final Long pmUserId
	) {
		final CursorPageRequest page = CursorPageRequest.of(cursor, size);
		return ApiResponse.success(
			projectQueryService.findPage(loginUser.userId(), page, status, departmentId, pmUserId)
				.map(ProjectSummaryResponse::from)
		);
	}

	@GetMapping("/{projectId}")
	public ApiResponse<ProjectDetailResponse> get(
		@AuthenticationPrincipal final LoginUser loginUser,
		@PathVariable @Positive final Long projectId
	) {
		return ApiResponse.success(ProjectDetailResponse.from(projectQueryService.getDetail(loginUser.userId(), projectId)));
	}

	/** 폭포수 단계·게이트(단계 순서대로, 항상 5개라 페이지네이션 없음). 다른 운영 방식이면 404 PHASES_NOT_AVAILABLE. */
	@GetMapping("/{projectId}/phases")
	public ApiResponse<List<ProjectPhaseResponse>> getPhases(
		@AuthenticationPrincipal final LoginUser loginUser,
		@PathVariable @Positive final Long projectId
	) {
		final ProjectPhasesResult result = projectQueryService.getPhases(loginUser.userId(), projectId);
		return ApiResponse.success(
			result.phases().stream().map(phase -> ProjectPhaseResponse.of(phase, result.userNames())).toList()
		);
	}

	/** 단계형 생성의 마지막에 한 번 호출한다. 응답은 생성된 프로젝트 상세다. */
	@PostMapping
	public ApiResponse<ProjectDetailResponse> create(
		final AuditActor actor,
		@Valid @RequestBody final CreateProjectRequest request
	) {
		final Long projectId = projectCreationService.create(actor, request.toCommand());
		return ApiResponse.success(ProjectDetailResponse.from(projectQueryService.getDetail(actor.userId(), projectId)));
	}

	/** 생성 화면 ②·③의 선택지(활성·필수 여부 포함). */
	@GetMapping("/creation-options")
	public ApiResponse<CreationOptionsResponse> creationOptions() {
		return ApiResponse.success(CreationOptionsResponse.current());
	}

	/** 생성 화면 ①의 코드 중복 확인. 프로젝트 생성 권한자만 호출할 수 있다. */
	@GetMapping("/code-availability")
	public ApiResponse<CodeAvailabilityResponse> codeAvailability(
		@AuthenticationPrincipal final LoginUser loginUser,
		@RequestParam @Pattern(regexp = CreateProjectRequest.CODE_PATTERN) final String code
	) {
		final String normalized = CreateProjectRequest.normalizeCode(code);
		return ApiResponse.success(new CodeAvailabilityResponse(
			normalized,
			projectCreationService.isCodeAvailable(loginUser.userId(), normalized)
		));
	}
}
