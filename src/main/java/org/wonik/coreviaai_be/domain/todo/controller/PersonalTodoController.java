package org.wonik.coreviaai_be.domain.todo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.todo.dto.request.CreateTodoRequest;
import org.wonik.coreviaai_be.domain.todo.dto.request.UpdateTodoRequest;
import org.wonik.coreviaai_be.domain.todo.dto.response.PersonalTodoResponse;
import org.wonik.coreviaai_be.domain.todo.dto.response.TodoItemResponse;
import org.wonik.coreviaai_be.domain.todo.service.PersonalTodoService;
import org.wonik.coreviaai_be.domain.todo.service.TodoFeedService;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.jwt.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me/todos")
@RequiredArgsConstructor
public class PersonalTodoController {
	private final PersonalTodoService personalService;
	private final TodoFeedService feedService;

	@GetMapping
	public ApiResponse<CursorPage<TodoItemResponse>> findPage(@AuthenticationPrincipal final LoginUser viewer,
		@RequestParam(required = false) final String cursor,
		@RequestParam(defaultValue = "20") @Min(1) @Max(CursorPageRequest.MAX_SIZE) final int size,
		@RequestParam(defaultValue = "false") final boolean done,
		@RequestParam @Min(1) @Max(365) final int dueSoonDays) {
		return ApiResponse.success(feedService.findPage(viewer.userId(),done,size,dueSoonDays,cursor).map(TodoItemResponse::from));
	}

	@GetMapping("/{todoId}")
	public ApiResponse<PersonalTodoResponse> get(@AuthenticationPrincipal final LoginUser viewer,
		@PathVariable @Positive final Long todoId) {
		return ApiResponse.success(PersonalTodoResponse.from(personalService.get(viewer.userId(),todoId)));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<PersonalTodoResponse> create(@AuthenticationPrincipal final LoginUser viewer,
		@Valid @RequestBody final CreateTodoRequest request) {
		return ApiResponse.success(PersonalTodoResponse.from(personalService.create(viewer.userId(),request.toCommand())));
	}

	@PatchMapping("/{todoId}")
	public ApiResponse<PersonalTodoResponse> update(@AuthenticationPrincipal final LoginUser viewer,
		@PathVariable @Positive final Long todoId, @Valid @RequestBody final UpdateTodoRequest request) {
		return ApiResponse.success(PersonalTodoResponse.from(personalService.update(viewer.userId(),todoId,request.toCommand())));
	}
}
