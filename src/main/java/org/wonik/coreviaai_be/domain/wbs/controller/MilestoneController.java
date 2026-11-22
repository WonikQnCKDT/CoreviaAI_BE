package org.wonik.coreviaai_be.domain.wbs.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.wbs.dto.request.CreateMilestoneRequest;
import org.wonik.coreviaai_be.domain.wbs.dto.request.ReplaceMilestoneTasksRequest;
import org.wonik.coreviaai_be.domain.wbs.dto.request.UpdateMilestoneRequest;
import org.wonik.coreviaai_be.domain.wbs.dto.response.MilestoneListResponse;
import org.wonik.coreviaai_be.domain.wbs.dto.response.MilestoneResponse;
import org.wonik.coreviaai_be.domain.wbs.service.MilestoneService;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.jwt.LoginUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * 마일스톤 API(FR-WBS-04). 조회는 프로젝트를 볼 수 있는 사용자 전원(볼 수 없으면 404), 변경은 PM만(3.3 WBS-04).
 * 판정은 서비스가 한다. /api/projects/** 경로라 시스템 관리자는 보안 설정에서 이미 차단된다.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/wbs/milestones")
@RequiredArgsConstructor
public class MilestoneController {

	private final MilestoneService milestoneService;

	@GetMapping
	public ApiResponse<MilestoneListResponse> findAll(
		@AuthenticationPrincipal final LoginUser loginUser,
		@PathVariable @Positive final Long projectId
	) {
		return ApiResponse.success(MilestoneListResponse.from(milestoneService.findAll(loginUser.userId(), projectId)));
	}

	@GetMapping("/{milestoneId}")
	public ApiResponse<MilestoneResponse> find(
		@AuthenticationPrincipal final LoginUser loginUser,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long milestoneId
	) {
		return ApiResponse.success(MilestoneResponse.from(milestoneService.find(loginUser.userId(), projectId, milestoneId)));
	}

	@PostMapping
	public ApiResponse<MilestoneResponse> create(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@Valid @RequestBody final CreateMilestoneRequest request
	) {
		return ApiResponse.success(MilestoneResponse.from(milestoneService.create(actor, projectId, request.toCommand())));
	}

	@PatchMapping("/{milestoneId}")
	public ApiResponse<MilestoneResponse> update(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long milestoneId,
		@Valid @RequestBody final UpdateMilestoneRequest request
	) {
		return ApiResponse.success(
			MilestoneResponse.from(milestoneService.update(actor, projectId, milestoneId, request.toCommand())));
	}

	/** 연결 작업 전체 교체. 같은 목록을 다시 보내면 아무것도 바꾸지 않고 성공한다(멱등). */
	@PutMapping("/{milestoneId}/tasks")
	public ApiResponse<MilestoneResponse> replaceTasks(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long milestoneId,
		@Valid @RequestBody final ReplaceMilestoneTasksRequest request
	) {
		return ApiResponse.success(
			MilestoneResponse.from(milestoneService.replaceTasks(actor, projectId, milestoneId, request.toCommand())));
	}

	@DeleteMapping("/{milestoneId}")
	public ApiResponse<Void> delete(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long milestoneId
	) {
		milestoneService.delete(actor, projectId, milestoneId);
		return ApiResponse.success(null);
	}
}
