package org.wonik.coreviaai_be.domain.notification.service;

import java.time.Clock;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.wonik.coreviaai_be.domain.notification.dto.response.NotificationResponse;
import org.wonik.coreviaai_be.domain.notification.entity.Notification;
import org.wonik.coreviaai_be.domain.notification.repository.NotificationRepository;
import org.wonik.coreviaai_be.domain.notification.service.command.PublishNotification;
import org.wonik.coreviaai_be.domain.notification.sse.NotificationStreamHub;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 인앱 알림 저장·조회·확인(FR-NTF-01).
 * <p>
 * 푸시는 커밋 뒤에만 보낸다. 업무 변경이 롤백되면 알림도 함께 사라지고, 연결이 없어도 목록 조회로 볼 수 있다.
 * 본인에게 한 배정·상태 변경은 알리지 않는다.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final NotificationStreamHub streamHub;
	private final JsonMapper jsonMapper;
	private final Clock clock;

	@Transactional
	public void publish(final PublishNotification command) {
		if (command.recipientUserId() == null) {
			return;
		}
		if (command.recipientUserId().equals(command.actorUserId())) {
			return;
		}
		final String params = toJson(command.messageParams());
		final Notification saved = notificationRepository.saveAndFlush(Notification.create(
			command.recipientUserId(),
			command.type(),
			command.projectId(),
			command.targetType(),
			command.targetId(),
			command.messageKey(),
			params,
			clock.instant()
		));
		final NotificationResponse response = toResponse(saved);
		afterCommit(() -> streamHub.publish(saved.getRecipientUserId(), response));
	}

	@Transactional(readOnly = true)
	public CursorPage<NotificationResponse> list(
		final Long recipientUserId,
		final CursorPageRequest page,
		final boolean unreadOnly
	) {
		final var fetched = notificationRepository.findPageForRecipient(
			recipientUserId,
			page.cursorId(),
			unreadOnly,
			page.fetchPageable()
		);
		return CursorPage.of(fetched, page.size(), Notification::getId).map(this::toResponse);
	}

	@Transactional(readOnly = true)
	public long unreadCount(final Long recipientUserId) {
		return notificationRepository.countByRecipientUserIdAndReadAtIsNull(recipientUserId);
	}

	@Transactional
	public NotificationResponse markRead(final Long recipientUserId, final Long notificationId) {
		final Notification notification = notificationRepository
			.findByIdAndRecipientUserId(notificationId, recipientUserId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));
		notification.markRead(clock.instant());
		return toResponse(notification);
	}

	private void afterCommit(final Runnable action) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			action.run();
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				action.run();
			}
		});
	}

	private String toJson(final Map<String, Object> messageParams) {
		if (messageParams == null || messageParams.isEmpty()) {
			return null;
		}
		return jsonMapper.writeValueAsString(messageParams);
	}

	private NotificationResponse toResponse(final Notification notification) {
		final JsonNode params = notification.getMessageParams() == null
			? null
			: jsonMapper.readTree(notification.getMessageParams());
		return NotificationResponse.from(notification, params);
	}
}
