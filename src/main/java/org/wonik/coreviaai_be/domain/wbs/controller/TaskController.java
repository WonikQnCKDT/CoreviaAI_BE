package org.wonik.coreviaai_be.domain.wbs.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.wbs.dto.request.UpdateTaskAttributesRequest;
import org.wonik.coreviaai_be.domain.wbs.dto.request.UpdateTaskProgressRequest;
import org.wonik.coreviaai_be.domain.wbs.dto.response.TaskDetailResponse;
import org.wonik.coreviaai_be.domain.wbs.dto.response.TaskHistoryResponse;
import org.wonik.coreviaai_be.domain.wbs.service.TaskDetailService;
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
 * 업무 API(FR-TSK-01, 05. 업무·보드). 업무 = WBS 작업·하위작업이라 같은 ID를 쓴다. 등록·기본 필드·담당자 배정·이동·삭제는
 * WBS API(/wbs/tasks, FR-WBS-01)가 하고, 여기서는 업무 상세 조회·상세 필드 수정·변경 이력 조회를 한다.
 * 권한 판정은 서비스가 한다. /api/projects/** 경로라 시스템 관리자는 보안 설정에서 이미 차단된다.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/tasks/{taskId}")
@RequiredArgsConstructor
public class TaskController {

	private final TaskDetailService taskDetailService;

	@GetMapping
	public ApiResponse<TaskDetailResponse> find(
		@AuthenticationPrincipal final LoginUser loginUser,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long taskId
	) {
		return ApiResponse.success(TaskDetailResponse.from(taskDetailService.find(loginUser.userId(), projectId, taskId)));
	}

	/** 유형·우선순위·완료 조건 수정. 같은 값을 다시 보내면 아무것도 바꾸지 않고 성공한다(멱등). */
	@PatchMapping
	public ApiResponse<TaskDetailResponse> updateAttributes(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long taskId,
		@Valid @RequestBody final UpdateTaskAttributesRequest request
	) {
		return ApiResponse.success(TaskDetailResponse.from(
			taskDetailService.updateAttributes(actor, projectId, taskId, request.toCommand())));
	}

	/** 진척률 수정. 하위작업이 있는 상위 작업은 직접 수정하지 않고 하위작업의 예상 공수 가중 평균을 쓴다. */
	@PatchMapping("/progress")
	public ApiResponse<TaskDetailResponse> updateProgress(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long taskId,
		@Valid @RequestBody final UpdateTaskProgressRequest request
	) {
		return ApiResponse.success(TaskDetailResponse.from(
			taskDetailService.updateProgress(actor, projectId, taskId, request.toCommand())));
	}

	/** 변경 이력(커서, id 오름차순 = 시간순). */
	@GetMapping("/histories")
	public ApiResponse<CursorPage<TaskHistoryResponse>> findHistories(
		@AuthenticationPrincipal final LoginUser loginUser,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long taskId,
		@RequestParam(required = false) final String cursor,
		@RequestParam(defaultValue = "" + CursorPageRequest.DEFAULT_SIZE)
		@Min(1) @Max(CursorPageRequest.MAX_SIZE) final int size
	) {
		final CursorPageRequest page = CursorPageRequest.of(cursor, size);
		return ApiResponse.success(taskDetailService.findHistories(loginUser.userId(), projectId, taskId, page)
			.map(TaskHistoryResponse::from));
	}
}
