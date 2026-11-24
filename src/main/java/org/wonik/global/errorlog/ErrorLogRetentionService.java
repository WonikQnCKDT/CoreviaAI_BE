package org.wonik.global.errorlog;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.ZoneOffset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

/** 보관 기간이 지난 로그만 정리한다. API를 통한 임의 삭제는 제공하지 않는다. */
@Service
@RequiredArgsConstructor
public class ErrorLogRetentionService {
    public static final int BATCH_SIZE = 1000;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    @Transactional
    public int deleteExpiredBatch() {
        final var cutoff = clock.instant().atZone(ZoneOffset.UTC).minusYears(1).toInstant();
        return jdbc.update("""
            delete from error_logs where id in (
                select id from error_logs where occurred_at < ?
                order by occurred_at, id limit ?
            )
            """, Timestamp.from(cutoff), BATCH_SIZE);
    }
}
