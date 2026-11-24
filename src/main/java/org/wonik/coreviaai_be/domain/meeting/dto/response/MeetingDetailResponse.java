package org.wonik.coreviaai_be.domain.meeting.dto.response;

import java.time.Instant;
import java.util.List;

import org.wonik.coreviaai_be.domain.meeting.service.result.MeetingDetailResult;

public record MeetingDetailResponse(
	Long id,
	Long projectId,
	String title,
	Instant heldAt,
	String content,
	Long authorUserId,
	String authorName,
	long version,
	List<MeetingAttendeeResponse> attendees,
	List<MeetingDecisionResponse> decisions,
	List<MeetingActionItemResponse> actionItems,
	Instant createdAt,
	Instant updatedAt
) {
	public static MeetingDetailResponse from(final MeetingDetailResult result) {
		return new MeetingDetailResponse(result.id(), result.projectId(), result.title(), result.heldAt(), result.content(),
			result.authorUserId(), result.authorName(), result.version(),
			result.attendees().stream().map(MeetingAttendeeResponse::from).toList(),
			result.decisions().stream().map(MeetingDecisionResponse::from).toList(),
			result.actionItems().stream().map(MeetingActionItemResponse::from).toList(),
			result.createdAt(), result.updatedAt());
	}
}
