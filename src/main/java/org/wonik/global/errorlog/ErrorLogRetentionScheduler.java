package org.wonik.global.errorlog;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 한 번에 최대 10,000건, 배치별 독립 트랜잭션. 다중 인스턴스에서도 기간 안의 행은 삭제하지 않는다. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.error-log-retention.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class ErrorLogRetentionScheduler {
    private static final int MAX_BATCHES = 10;
    private final ErrorLogRetentionService retention;

    @Scheduled(cron = "${app.error-log-retention.cron:0 0 * * * *}", zone = "UTC")
    public void cleanExpiredLogs() {
        int deleted = 0;
        for (int batch = 0; batch < MAX_BATCHES; batch++) {
            final int count = retention.deleteExpiredBatch();
            deleted += count;
            if (count < ErrorLogRetentionService.BATCH_SIZE) break;
        }
        log.info("Error log retention completed: deleted={}", deleted);
    }
}
