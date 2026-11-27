package org.wonik.coreviaai_be.domain.notification.entity;

import java.time.Instant;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인앱 알림(FR-NTF-01). 추가만 하고 본문은 고치지 않는다. 확인은 read_at만 채운다.
 */
@Getter
@Entity
@Table(name = "notifications")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "recipient_user_id", nullable = false, updatable = false)
	private Long recipientUserId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "notification_type", nullable = false, updatable = false, length = 30)
	private NotificationType notificationType;

	@Column(name = "project_id", updatable = false)
	private Long projectId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "target_type", updatable = false, length = 30)
	private NotificationTargetType targetType;

	@Column(name = "target_id", updatable = false)
	private Long targetId;

	@Column(name = "message_key", nullable = false, updatable = false, length = 100)
	private String messageKey;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "message_params", updatable = false)
	private String messageParams;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "channel", nullable = false, updatable = false, length = 20)
	private NotificationChannel channel;

	@Column(name = "read_at")
	private Instant readAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	private Notification(
		final Long recipientUserId,
		final NotificationType notificationType,
		final Long projectId,
		final NotificationTargetType targetType,
		final Long targetId,
		final String messageKey,
		final String messageParams,
		final Instant createdAt
	) {
		this.recipientUserId = recipientUserId;
		this.notificationType = notificationType;
		this.projectId = projectId;
		this.targetType = targetType;
		this.targetId = targetId;
		this.messageKey = messageKey;
		this.messageParams = messageParams;
		this.channel = NotificationChannel.IN_APP;
		this.createdAt = createdAt;
	}

	public static Notification create(
		final Long recipientUserId,
		final NotificationType notificationType,
		final Long projectId,
		final NotificationTargetType targetType,
		final Long targetId,
		final String messageKey,
		final String messageParams,
		final Instant createdAt
	) {
		return new Notification(
			recipientUserId, notificationType, projectId, targetType, targetId, messageKey, messageParams, createdAt
		);
	}

	public boolean isUnread() {
		return readAt == null;
	}

	/** 이미 확인한 알림은 시각을 바꾸지 않는다(재전송에 멱등). */
	public void markRead(final Instant readAt) {
		if (this.readAt == null) {
			this.readAt = readAt;
		}
	}
}
