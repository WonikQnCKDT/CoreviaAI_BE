package org.wonik.coreviaai_be.domain.meeting.dto.response;

import org.wonik.coreviaai_be.domain.meeting.service.result.MeetingAttendeeResult;

public record MeetingAttendeeResponse(Long id, Long userId, String name, boolean external) {
	public static MeetingAttendeeResponse from(final MeetingAttendeeResult result) {
		return new MeetingAttendeeResponse(result.id(), result.userId(), result.name(), result.external());
	}
}
