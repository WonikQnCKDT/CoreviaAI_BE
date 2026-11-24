package org.wonik.coreviaai_be.domain.worklog.entity;

import java.util.Objects;
import jakarta.persistence.*;
import lombok.*;
import org.wonik.global.common.entity.BaseTimeEntity;

@Entity
@Table(name = "worklog_tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorklogTask extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "worklog_id", nullable = false, updatable = false)
    private Long worklogId;
    @Column(name = "task_id", nullable = false, updatable = false)
    private Long taskId;
    private String content;
    private int progress;
    @Column(name = "spent_minutes", nullable = false)
    private int spentMinutes;
    private boolean completed;
    @Version private long version;

    public static WorklogTask create(Long worklogId, Long taskId) {
        final var entry = new WorklogTask();
        entry.worklogId = worklogId;
        entry.taskId = taskId;
        return entry;
    }
    public boolean matches(String content, int progress, int minutes, boolean completed) {
        return Objects.equals(this.content, content) && this.progress == progress
            && spentMinutes == minutes && this.completed == completed;
    }
    public void replace(String content, int progress, int minutes, boolean completed) {
        this.content = content;
        this.progress = progress;
        this.spentMinutes = minutes;
        this.completed = completed;
    }
}
