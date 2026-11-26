package org.wonik.coreviaai_be.domain.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationTest {

	@Test
	@DisplayName("확인 시각은 처음 한 번만 기록한다")
	void markReadIsIdempotent() {
		final Notification notification = Notification.create(
			1L, NotificationType.ASSIGNED, 2L, NotificationTargetType.TASK, 3L,
			"notification.assigned", null, Instant.parse("2026-12-01T00:00:00Z")
		);
		final Instant first = Instant.parse("2026-12-09T00:00:00Z");
		notification.markRead(first);
		notification.markRead(first.plusSeconds(60));

		assertThat(notification.isUnread()).isFalse();
		assertThat(notification.getReadAt()).isEqualTo(first);
	}
}
