package org.wonik.coreviaai_be.domain.worklog.dto.request;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.wonik.coreviaai_be.domain.worklog.service.command.WorklogTaskCommand;
public record WorklogTaskRequest(@NotNull @Positive Long projectId, @Size(max = 10000) String content,
    @NotNull @Min(0) @Max(100) Integer progress, @NotNull @Min(0) @Max(1440) Integer spentMinutes,
    @NotNull Boolean completed, @NotNull @PositiveOrZero Long taskVersion,
    @NotNull @PositiveOrZero Long version) {
    public WorklogTaskCommand toCommand() {
        return new WorklogTaskCommand(projectId, content, progress, spentMinutes, completed, taskVersion, version);
    }
}
