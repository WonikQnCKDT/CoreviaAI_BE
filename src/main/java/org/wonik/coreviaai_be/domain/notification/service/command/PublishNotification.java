package org.wonik.coreviaai_be.domain.notification.service.command;

import java.util.Map;

import org.wonik.coreviaai_be.domain.notification.entity.NotificationTargetType;
import org.wonik.coreviaai_be.domain.notification.entity.NotificationType;

/**
 * 알림 한 건을 만들 때 필요한 값. actorUserId와 수신자가 같으면 저장하지 않는다.
 * actorUserId가 null이면(스케줄러) 그 비교를 하지 않는다.
 */
public record PublishNotification(
	Long recipientUserId,
	Long actorUserId,
	NotificationType type,
	Long projectId,
	NotificationTargetType targetType,
	Long targetId,
	String messageKey,
	Map<String, Object> messageParams
) {
}
