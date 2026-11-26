package org.wonik.coreviaai_be.domain.notification.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.notification.entity.NotificationTargetType;
import org.wonik.coreviaai_be.domain.notification.entity.NotificationType;
import org.wonik.coreviaai_be.domain.notification.repository.NotificationRepository;
import org.wonik.coreviaai_be.domain.notification.service.command.PublishNotification;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.global.common.time.BusinessCalendar;

import lombok.RequiredArgsConstructor;

/**
 * 마감이 다음 업무일(Asia/Seoul)인 미완료 작업의 담당자에게 D-1 알림을 만든다.
 * 같은 날 다시 실행해도 담당자·작업당 한 건만 남긴다.
 */
@Service
@RequiredArgsConstructor
public class DueSoonNotificationPublisher {

	static final String MESSAGE_KEY = "notification.dueSoon";

	private final WbsTaskRepository taskRepository;
	private final NotificationRepository notificationRepository;
	private final NotificationService notificationService;
	private final Clock clock;

	@Transactional
	public void publishDueTomorrow() {
		final LocalDate tomorrow = BusinessCalendar.today(clock).plusDays(1);
		final Instant startOfToday = BusinessCalendar.today(clock).atStartOfDay(BusinessCalendar.ZONE).toInstant();
		for (final WbsTask task : taskRepository.findDueOn(tomorrow, WbsTaskStatus.DONE)) {
			final boolean alreadySent = notificationRepository
				.existsByRecipientUserIdAndNotificationTypeAndTargetTypeAndTargetIdAndCreatedAtGreaterThanEqual(
					task.getAssigneeUserId(),
					NotificationType.DUE_SOON,
					NotificationTargetType.TASK,
					task.getId(),
					startOfToday
				);
			if (alreadySent) {
				continue;
			}
			notificationService.publish(new PublishNotification(
				task.getAssigneeUserId(),
				null,
				NotificationType.DUE_SOON,
				task.getProjectId(),
				NotificationTargetType.TASK,
				task.getId(),
				MESSAGE_KEY,
				Map.of("taskName", task.getName(), "endDate", task.getEndDate().toString())
			));
		}
	}
}
