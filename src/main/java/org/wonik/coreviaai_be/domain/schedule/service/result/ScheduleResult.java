package org.wonik.coreviaai_be.domain.schedule.service.result;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.schedule.entity.PersonalEvent;

public record ScheduleResult(Long id, String title, Instant startAt, Instant endAt, boolean allDay, String memo,
	Instant createdAt, Instant updatedAt) {

	public static ScheduleResult from(final PersonalEvent event) {
		return new ScheduleResult(event.getId(), event.getTitle(), event.getStartAt(), event.getEndAt(),
			event.isAllDay(), event.getMemo(), event.getCreatedAt(), event.getUpdatedAt());
	}
}
