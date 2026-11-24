package org.wonik.coreviaai_be.domain.meeting.dto.request;

import org.wonik.coreviaai_be.domain.meeting.entity.MeetingAttendee;
import org.wonik.coreviaai_be.domain.meeting.service.command.MeetingAttendeeCommand;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MeetingAttendeeRequest(
	@Positive Long userId,
	@Size(max = MeetingAttendee.EXTERNAL_NAME_MAX_LENGTH) String externalName
) {
	public MeetingAttendeeCommand toCommand() {
		return new MeetingAttendeeCommand(userId, externalName);
	}
}
