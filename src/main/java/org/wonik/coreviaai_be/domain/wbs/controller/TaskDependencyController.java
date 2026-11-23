package org.wonik.coreviaai_be.domain.wbs.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.wbs.dto.request.ReplaceTaskPredecessorsRequest;
import org.wonik.coreviaai_be.domain.wbs.dto.response.TaskDependenciesResponse;
import org.wonik.coreviaai_be.domain.wbs.service.TaskDependencyService;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.jwt.LoginUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/** 완료→시작 선후행 관계 API(FR-WBS-03). */
@RestController
@RequestMapping("/api/projects/{projectId}/wbs/tasks/{taskId}/predecessors")
@RequiredArgsConstructor
public class TaskDependencyController {

	private final TaskDependencyService dependencyService;

	@GetMapping
	public ApiResponse<TaskDependenciesResponse> find(
		@AuthenticationPrincipal final LoginUser loginUser,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long taskId
	) {
		return ApiResponse.success(TaskDependenciesResponse.from(
			dependencyService.find(loginUser.userId(), projectId, taskId)));
	}

	@PutMapping
	public ApiResponse<TaskDependenciesResponse> replace(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long taskId,
		@Valid @RequestBody final ReplaceTaskPredecessorsRequest request
	) {
		return ApiResponse.success(TaskDependenciesResponse.from(
			dependencyService.replace(actor, projectId, taskId, request.toCommand())));
	}
}
