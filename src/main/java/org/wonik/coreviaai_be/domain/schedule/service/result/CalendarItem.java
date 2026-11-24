package org.wonik.coreviaai_be.domain.schedule.service.result;
import java.time.Instant;
public record CalendarItem(Long id, String source, String title, Instant startAt, Instant endAt,
    boolean allDay, Long projectId, String taskStatus, Integer progress, Long spentMinutes) {}
