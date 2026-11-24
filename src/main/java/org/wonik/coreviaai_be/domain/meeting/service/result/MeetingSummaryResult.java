package org.wonik.coreviaai_be.domain.meeting.service.result;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.meeting.entity.Meeting;

public record MeetingSummaryResult(
	Long id,
	String title,
	Instant heldAt,
	Long authorUserId,
	String authorName,
	long version,
	Instant createdAt,
	Instant updatedAt
) {
	public static MeetingSummaryResult from(final Meeting meeting, final String authorName) {
		return new MeetingSummaryResult(meeting.getId(), meeting.getTitle(), meeting.getHeldAt(),
			meeting.getAuthorUserId(), authorName, meeting.getVersion(), meeting.getCreatedAt(), meeting.getUpdatedAt());
	}
}
