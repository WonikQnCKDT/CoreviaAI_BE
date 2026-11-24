package org.wonik.coreviaai_be.domain.meeting.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.wonik.coreviaai_be.domain.meeting.dto.request.CreateMeetingRequest;
import org.wonik.coreviaai_be.domain.meeting.dto.request.UpdateMeetingRequest;
import org.wonik.coreviaai_be.domain.meeting.dto.response.MeetingDetailResponse;
import org.wonik.coreviaai_be.domain.meeting.dto.response.MeetingSummaryResponse;
import org.wonik.coreviaai_be.domain.meeting.service.MeetingService;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.jwt.LoginUser;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/projects/{projectId}/meetings")
@RequiredArgsConstructor
public class MeetingController {

	private final MeetingService meetingService;

	@GetMapping
	public ApiResponse<CursorPage<MeetingSummaryResponse>> findPage(
		@AuthenticationPrincipal final LoginUser viewer,
		@PathVariable @Positive final Long projectId,
		@RequestParam(required = false) @Size(max = 200) final String keyword,
		@RequestParam(required = false) final String cursor,
		@RequestParam(defaultValue = "20") @Min(1) @Max(CursorPageRequest.MAX_SIZE) final int size
	) {
		return ApiResponse.success(meetingService.findPage(viewer.userId(), projectId, keyword, cursor, size)
			.map(MeetingSummaryResponse::from));
	}

	@GetMapping("/{meetingId}")
	public ApiResponse<MeetingDetailResponse> get(
		@AuthenticationPrincipal final LoginUser viewer,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long meetingId
	) {
		return ApiResponse.success(MeetingDetailResponse.from(meetingService.get(viewer.userId(), projectId, meetingId)));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<MeetingDetailResponse> create(
		@AuthenticationPrincipal final LoginUser viewer,
		@PathVariable @Positive final Long projectId,
		@Valid @RequestBody final CreateMeetingRequest request
	) {
		return ApiResponse.success(MeetingDetailResponse.from(
			meetingService.create(viewer.userId(), projectId, request.toCommand())));
	}

	@PatchMapping("/{meetingId}")
	public ApiResponse<MeetingDetailResponse> update(
		@AuthenticationPrincipal final LoginUser viewer,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long meetingId,
		@Valid @RequestBody final UpdateMeetingRequest request
	) {
		return ApiResponse.success(MeetingDetailResponse.from(
			meetingService.update(viewer.userId(), projectId, meetingId, request.version(), request.toCommand())));
	}

	@DeleteMapping("/{meetingId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
		final AuditActor actor,
		@PathVariable @Positive final Long projectId,
		@PathVariable @Positive final Long meetingId
	) {
		meetingService.delete(actor, projectId, meetingId);
	}
}
