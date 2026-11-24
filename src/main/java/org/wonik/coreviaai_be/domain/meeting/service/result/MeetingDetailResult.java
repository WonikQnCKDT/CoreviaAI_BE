package org.wonik.coreviaai_be.domain.meeting.service.result;

import java.time.Instant;
import java.util.List;

public record MeetingDetailResult(
	Long id,
	Long projectId,
	String title,
	Instant heldAt,
	String content,
	Long authorUserId,
	String authorName,
	long version,
	List<MeetingAttendeeResult> attendees,
	List<MeetingDecisionResult> decisions,
	List<MeetingActionItemResult> actionItems,
	Instant createdAt,
	Instant updatedAt
) {
}
