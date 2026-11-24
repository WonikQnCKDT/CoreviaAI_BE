package org.wonik.coreviaai_be.domain.worklog.service.result;
import java.time.LocalDate;
import org.wonik.coreviaai_be.domain.worklog.entity.WorklogTask;
public record WorklogTaskResult(Long id, Long taskId, Long userId, LocalDate logDate, String content,
    int progress, int spentMinutes, boolean completed, long version) {
    public static WorklogTaskResult from(WorklogTask entry, Long userId, LocalDate date) {
        return new WorklogTaskResult(entry.getId(), entry.getTaskId(), userId, date, entry.getContent(),
            entry.getProgress(), entry.getSpentMinutes(), entry.isCompleted(), entry.getVersion());
    }
}
