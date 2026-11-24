package org.wonik.coreviaai_be.domain.meeting.dto.request;

import java.time.Instant;
import java.util.List;

import org.wonik.coreviaai_be.domain.meeting.entity.Meeting;
import org.wonik.coreviaai_be.domain.meeting.entity.MeetingDecision;
import org.wonik.coreviaai_be.domain.meeting.service.MeetingService;
import org.wonik.coreviaai_be.domain.meeting.service.command.SaveMeetingCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateMeetingRequest(
	@NotNull @PositiveOrZero Long version,
	@NotBlank @Size(max = Meeting.TITLE_MAX_LENGTH) String title,
	@NotNull Instant heldAt,
	@Size(max = Meeting.CONTENT_MAX_LENGTH) String content,
	@NotNull @Size(max = MeetingService.MAX_ATTENDEES) List<@NotNull @Valid MeetingAttendeeRequest> attendees,
	@NotNull @Size(max = MeetingService.MAX_DECISIONS)
		List<@NotBlank @Size(max = MeetingDecision.CONTENT_MAX_LENGTH) String> decisions,
	@NotNull @Size(max = MeetingService.MAX_ACTION_ITEMS) List<@NotNull @Valid MeetingActionItemRequest> actionItems
) {
	public SaveMeetingCommand toCommand() {
		return new SaveMeetingCommand(title, heldAt, content, attendees.stream().map(MeetingAttendeeRequest::toCommand).toList(),
			decisions, actionItems.stream().map(MeetingActionItemRequest::toCommand).toList());
	}
}
