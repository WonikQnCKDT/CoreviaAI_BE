package org.wonik.coreviaai_be.domain.worklog.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.worklog.dto.request.UpsertWorklogRequest;
import org.wonik.coreviaai_be.domain.worklog.dto.response.WorklogResponse;
import org.wonik.coreviaai_be.domain.worklog.service.WorklogService;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.jwt.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me/worklogs")
@RequiredArgsConstructor
public class WorklogController {

	private final WorklogService worklogService;

	/** [from, to] 날짜 기간의 본인 업무일지. 기간은 366일 이내. 내용이 없는 날은 목록에 없다. */
	@GetMapping
	public ApiResponse<List<WorklogResponse>> findRange(@AuthenticationPrincipal final LoginUser viewer,
		@RequestParam final LocalDate from, @RequestParam final LocalDate to) {
		return ApiResponse.success(worklogService.findRange(viewer.userId(), from, to).stream()
			.map(WorklogResponse::from).toList());
	}

	@GetMapping("/{logDate}")
	public ApiResponse<WorklogResponse> get(@AuthenticationPrincipal final LoginUser viewer,
		@PathVariable final LocalDate logDate) {
		return ApiResponse.success(WorklogResponse.from(worklogService.get(viewer.userId(), logDate)));
	}

	/** 그날 업무일지를 만들거나 내용을 바꾼다. 같은 요청을 다시 보내도 결과가 같다. */
	@PutMapping("/{logDate}")
	public ApiResponse<WorklogResponse> upsert(@AuthenticationPrincipal final LoginUser viewer,
		@PathVariable final LocalDate logDate, @Valid @RequestBody final UpsertWorklogRequest request) {
		return ApiResponse.success(WorklogResponse.from(
			worklogService.upsert(viewer.userId(), logDate, request.content())));
	}
}
