package org.wonik.global.errorlog;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.*;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.support.ApiIntegrationTestSupport;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorLogRetentionIntegrationTest extends ApiIntegrationTestSupport {
    @Test void keepsBoundaryAndRecentRowsWhileExpiredDeletionIsIdempotent() {
        Instant now = Instant.parse("2028-02-29T12:00:00Z");
        Instant boundary = Instant.parse("2027-02-28T12:00:00Z");
        for (Instant time : new Instant[] {boundary.minusSeconds(1), boundary, now}) {
            jdbcTemplate.update("insert into error_logs(occurred_at, error_type) values (?, ?)", Timestamp.from(time), "test");
        }
        var service = new ErrorLogRetentionService(jdbcTemplate, Clock.fixed(now, ZoneOffset.UTC));
        assertThat(service.deleteExpiredBatch()).isEqualTo(1);
        assertThat(service.deleteExpiredBatch()).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from error_logs", Integer.class)).isEqualTo(2);
    }
}
