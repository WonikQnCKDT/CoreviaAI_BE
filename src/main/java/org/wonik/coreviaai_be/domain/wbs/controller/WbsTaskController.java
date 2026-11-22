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
import org.wonik.coreviaai_be.domain.wbs.dto.request.CreateWbsTaskRequest;
import org.wonik.coreviaai_be.domain.wbs.dto.request.MoveWbsTaskRequest;
import org.wonik.coreviaai_be.domain.wbs.dto.request.UpdateWbsTaskRequest;
import org.wonik.coreviaai_be.domain.wbs.dto.response.WbsTaskChangeResponse;
import org.wonik.coreviaai_be.domain.wbs.dto.response.WbsTreeResponse;
import org.wonik.coreviaai_be.domain.wbs.service.WbsTaskService;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.jwt.LoginUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * 계층형 WBS API(FR-WBS-01). 조회는 프로젝트를 볼 수 있는 사용자 모두(볼 수 없으면 404), 변경 권한은
 * 3.3 권한 매트릭스 WBS-01을 따른다(PM 전체, 개발자·QA는 본인 담당 작업 수정·그 아래 하위작업 추가). 판정은 서비스가 한다.
 * /api/projects/** 경로라 시스템 관리자는 보안 설정에서 이미 차단된다.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/wbs/tasks")
@RequiredArgsConstructor
public class WbsTaskController {

	private final WbsTaskService taskService;

	@GetMapping
	public ApiResponse<WbsTreeResponse> findTree(
		@AuthenticationPrincipal final LoginUser loginUser,
		@PathVariable @Positive final Long projectId
	) {
		return ApiResponse.success(WbsTreeResponse.from(taskService.findTree(loginUser.userId(), projectId)));
	}

	@PostMapping
	public ApiResponse<WbsTaskChangeResponse> create(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@Valid @RequestBody final CreateWbsTaskRequest request
	) {
		return ApiResponse.success(WbsTaskChangeResponse.from(taskService.create(actor, projectId, request.toCommand())));
	}

	@PatchMapping("/{taskId}")
	public ApiResponse<WbsTaskChangeResponse> update(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long taskId,
		@Valid @RequestBody final UpdateWbsTaskRequest request
	) {
		return ApiResponse.success(
			WbsTaskChangeResponse.from(taskService.update(actor, projectId, taskId, request.toCommand())));
	}

	/** 순서·계층 변경(드래그). 같은 위치로 다시 보내면 아무것도 바꾸지 않고 성공한다(멱등). */
	@PutMapping("/{taskId}/position")
	public ApiResponse<WbsTaskChangeResponse> move(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long taskId,
		@Valid @RequestBody final MoveWbsTaskRequest request
	) {
		return ApiResponse.success(
			WbsTaskChangeResponse.from(taskService.move(actor, projectId, taskId, request.toCommand())));
	}

	@DeleteMapping("/{taskId}")
	public ApiResponse<WbsTaskChangeResponse> delete(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long taskId
	) {
		return ApiResponse.success(WbsTaskChangeResponse.from(taskService.delete(actor, projectId, taskId)));
	}
}
