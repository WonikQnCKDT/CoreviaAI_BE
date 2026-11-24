package org.wonik.coreviaai_be.domain.meeting.service.command;

import java.time.Instant;
import java.util.List;

public record SaveMeetingCommand(
	String title,
	Instant heldAt,
	String content,
	List<MeetingAttendeeCommand> attendees,
	List<String> decisions,
	List<MeetingActionItemCommand> actionItems
) {
}
