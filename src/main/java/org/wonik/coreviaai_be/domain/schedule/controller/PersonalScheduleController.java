package org.wonik.coreviaai_be.domain.schedule.controller;

import java.time.Instant;
import java.util.List;

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
import org.wonik.coreviaai_be.domain.schedule.dto.request.ScheduleRequest;
import org.wonik.coreviaai_be.domain.schedule.dto.response.ScheduleResponse;
import org.wonik.coreviaai_be.domain.schedule.service.PersonalScheduleService;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.jwt.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/me/events")
@RequiredArgsConstructor
public class PersonalScheduleController {

	private final PersonalScheduleService scheduleService;

	/** [from, to) 기간과 겹치는 본인 일정. 기간은 366일 이내. */
	@GetMapping
	public ApiResponse<List<ScheduleResponse>> findOverlapping(@AuthenticationPrincipal final LoginUser viewer,
		@RequestParam final Instant from, @RequestParam final Instant to) {
		return ApiResponse.success(scheduleService.findOverlapping(viewer.userId(), from, to).stream()
			.map(ScheduleResponse::from).toList());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ScheduleResponse> create(@AuthenticationPrincipal final LoginUser viewer,
		@Valid @RequestBody final ScheduleRequest request) {
		return ApiResponse.success(ScheduleResponse.from(scheduleService.create(viewer.userId(), request.toCommand())));
	}

	@PatchMapping("/{eventId}")
	public ApiResponse<ScheduleResponse> update(@AuthenticationPrincipal final LoginUser viewer,
		@PathVariable @Positive final Long eventId, @Valid @RequestBody final ScheduleRequest request) {
		return ApiResponse.success(ScheduleResponse.from(
			scheduleService.update(viewer.userId(), eventId, request.toCommand())));
	}
}
