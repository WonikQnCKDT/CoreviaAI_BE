package org.wonik.coreviaai_be.domain.notification.controller;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.wonik.coreviaai_be.domain.notification.dto.response.NotificationResponse;
import org.wonik.coreviaai_be.domain.notification.dto.response.UnreadCountResponse;
import org.wonik.coreviaai_be.domain.notification.service.NotificationService;
import org.wonik.coreviaai_be.domain.notification.sse.NotificationStreamHub;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.jwt.LoginUser;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * 로그인한 사용자 본인의 인앱 알림(FR-NTF-01). 다른 사람의 알림은 없는 알림과 같이 404다.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;
	private final NotificationStreamHub streamHub;

	@GetMapping
	public ApiResponse<CursorPage<NotificationResponse>> list(
		@AuthenticationPrincipal final LoginUser viewer,
		@RequestParam(required = false) final String cursor,
		@RequestParam(defaultValue = "" + CursorPageRequest.DEFAULT_SIZE)
		@Min(1) @Max(CursorPageRequest.MAX_SIZE) final int size,
		@RequestParam(defaultValue = "false") final boolean unreadOnly
	) {
		return ApiResponse.success(notificationService.list(
			viewer.userId(),
			CursorPageRequest.of(cursor, size),
			unreadOnly
		));
	}

	@GetMapping("/unread-count")
	public ApiResponse<UnreadCountResponse> unreadCount(@AuthenticationPrincipal final LoginUser viewer) {
		return ApiResponse.success(new UnreadCountResponse(notificationService.unreadCount(viewer.userId())));
	}

	@PostMapping("/{notificationId}/read")
	public ApiResponse<NotificationResponse> markRead(
		@AuthenticationPrincipal final LoginUser viewer,
		@PathVariable @Positive final Long notificationId
	) {
		return ApiResponse.success(notificationService.markRead(viewer.userId(), notificationId));
	}

	@GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(@AuthenticationPrincipal final LoginUser viewer) {
		return streamHub.subscribe(viewer.userId());
	}
}
