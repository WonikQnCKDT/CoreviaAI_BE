package org.wonik.coreviaai_be.domain.schedule.dto.response;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.schedule.service.result.ScheduleResult;

public record ScheduleResponse(Long id, String title, Instant startAt, Instant endAt, boolean allDay, String memo,
	Instant createdAt, Instant updatedAt) {

	public static ScheduleResponse from(final ScheduleResult result) {
		return new ScheduleResponse(result.id(), result.title(), result.startAt(), result.endAt(), result.allDay(),
			result.memo(), result.createdAt(), result.updatedAt());
	}
}
