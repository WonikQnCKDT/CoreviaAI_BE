package org.wonik.coreviaai_be.domain.project.controller;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;
import org.wonik.coreviaai_be.domain.project.dto.request.CreateProjectRequest;
import org.wonik.coreviaai_be.domain.project.dto.request.CreateProjectPhaseRequest;
import org.wonik.coreviaai_be.domain.project.dto.request.RenameProjectPhaseRequest;
import org.wonik.coreviaai_be.domain.project.dto.request.ReorderProjectPhasesRequest;
import org.wonik.coreviaai_be.domain.project.dto.response.CodeAvailabilityResponse;
import org.wonik.coreviaai_be.domain.project.dto.response.CreationOptionsResponse;
import org.wonik.coreviaai_be.domain.project.dto.response.ProjectDetailResponse;
import org.wonik.coreviaai_be.domain.project.dto.response.ProjectPhaseResponse;
import org.wonik.coreviaai_be.domain.project.dto.response.ProjectSummaryResponse;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectStatus;
import org.wonik.coreviaai_be.domain.project.service.ProjectCreationService;
import org.wonik.coreviaai_be.domain.project.service.ProjectPhaseService;
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
 * 생성 가능 여부(활성 일반 사용자)와 공개 범위 판정은 서비스가 한다.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

	private final ProjectCreationService projectCreationService;
	private final ProjectQueryService projectQueryService;
	private final ProjectPhaseService projectPhaseService;

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

	/** 프로젝트 단계·게이트(사용자 정의 순서, 최대 20개). 단계가 없으면 빈 배열이다. */
	@GetMapping("/{projectId}/phases")
	public ApiResponse<List<ProjectPhaseResponse>> getPhases(
		@AuthenticationPrincipal final LoginUser loginUser,
		@PathVariable @Positive final Long projectId
	) {
		final ProjectPhasesResult result = projectQueryService.getPhases(loginUser.userId(), projectId);
		return ApiResponse.success(
			result.phases().stream().map(phase -> ProjectPhaseResponse.of(phase, result.userNames(),
				result.progressByPhaseId().getOrDefault(phase.getId(), 0))).toList()
		);
	}

	@PostMapping("/{projectId}/phases")
	public ApiResponse<ProjectPhaseResponse> createPhase(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@Valid @RequestBody final CreateProjectPhaseRequest request
	) {
		return ApiResponse.success(ProjectPhaseResponse.of(
			projectPhaseService.create(actor, projectId, request.name()), Map.of()));
	}

	@PatchMapping("/{projectId}/phases/{phaseId}")
	public ApiResponse<Void> renamePhase(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long phaseId,
		@Valid @RequestBody final RenameProjectPhaseRequest request
	) {
		projectPhaseService.rename(actor, projectId, phaseId, request.name());
		return ApiResponse.success(null);
	}

	@PutMapping("/{projectId}/phases/order")
	public ApiResponse<Void> reorderPhases(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@Valid @RequestBody final ReorderProjectPhasesRequest request
	) {
		projectPhaseService.reorder(actor, projectId, request.phaseIds());
		return ApiResponse.success(null);
	}

	@DeleteMapping("/{projectId}/phases/{phaseId}")
	public ApiResponse<Void> deletePhase(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long phaseId
	) {
		projectPhaseService.delete(actor, projectId, phaseId);
		return ApiResponse.success(null);
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

	/** 생성 화면 ②·③·④의 선택지(템플릿 기본값, 활성·필수 여부 포함). */
	@GetMapping("/creation-options")
	public ApiResponse<CreationOptionsResponse> creationOptions() {
		return ApiResponse.success(CreationOptionsResponse.current());
	}

	/** 생성 화면 ①의 코드 중복 확인. 프로젝트를 생성할 수 있는 사용자(활성 일반 사용자)만 호출할 수 있다. */
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
