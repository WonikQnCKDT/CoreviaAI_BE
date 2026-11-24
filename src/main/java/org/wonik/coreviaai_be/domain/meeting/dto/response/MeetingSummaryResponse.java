package org.wonik.coreviaai_be.domain.meeting.dto.response;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.meeting.service.result.MeetingSummaryResult;

public record MeetingSummaryResponse(
	Long id,
	String title,
	Instant heldAt,
	Long authorUserId,
	String authorName,
	long version,
	Instant createdAt,
	Instant updatedAt
) {
	public static MeetingSummaryResponse from(final MeetingSummaryResult result) {
		return new MeetingSummaryResponse(result.id(), result.title(), result.heldAt(), result.authorUserId(),
			result.authorName(), result.version(), result.createdAt(), result.updatedAt());
	}
}
