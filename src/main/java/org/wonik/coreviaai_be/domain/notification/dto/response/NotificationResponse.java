package org.wonik.coreviaai_be.domain.notification.dto.response;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.notification.entity.Notification;
import org.wonik.coreviaai_be.domain.notification.entity.NotificationChannel;
import org.wonik.coreviaai_be.domain.notification.entity.NotificationTargetType;
import org.wonik.coreviaai_be.domain.notification.entity.NotificationType;

import tools.jackson.databind.JsonNode;

public record NotificationResponse(
	Long id,
	NotificationType notificationType,
	Long projectId,
	NotificationTargetType targetType,
	Long targetId,
	String messageKey,
	JsonNode messageParams,
	NotificationChannel channel,
	Instant readAt,
	Instant createdAt
) {

	public static NotificationResponse from(final Notification notification, final JsonNode messageParams) {
		return new NotificationResponse(
			notification.getId(),
			notification.getNotificationType(),
			notification.getProjectId(),
			notification.getTargetType(),
			notification.getTargetId(),
			notification.getMessageKey(),
			messageParams,
			notification.getChannel(),
			notification.getReadAt(),
			notification.getCreatedAt()
		);
	}
}
