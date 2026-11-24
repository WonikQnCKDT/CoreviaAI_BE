package org.wonik.coreviaai_be.domain.meeting.service.result;

public record MeetingAttendeeResult(Long id, Long userId, String name, boolean external) {
}
