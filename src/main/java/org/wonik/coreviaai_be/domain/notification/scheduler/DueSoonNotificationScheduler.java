package org.wonik.coreviaai_be.domain.notification.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.wonik.coreviaai_be.domain.notification.service.DueSoonNotificationPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 매일 한국 시간 00:10에 마감 D-1 알림을 만든다. cron·사용 여부는 app.notification.due-soon.* */
@Component
@ConditionalOnProperty(name = "app.notification.due-soon.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DueSoonNotificationScheduler {

	private final DueSoonNotificationPublisher publisher;

	@Scheduled(cron = "${app.notification.due-soon.cron:0 10 0 * * *}", zone = "Asia/Seoul")
	public void notifyTasksDueTomorrow() {
		try {
			publisher.publishDueTomorrow();
		} catch (final RuntimeException exception) {
			log.error("마감 D-1 알림 생성이 중단되었습니다", exception);
		}
	}
}
