package org.wonik.coreviaai_be.domain.notification.entity;

/** 인앱 알림 종류(notifications.notification_type). 메일 채널은 후속(FR-NTF-02)이다. */
public enum NotificationType {
	ASSIGNED,
	MENTIONED,
	STATUS_CHANGED,
	DUE_SOON,
	APPROVAL_REQUESTED,
	MISSING_TASK,
	RISK_UP,
	PROJECT_INACTIVE
}
