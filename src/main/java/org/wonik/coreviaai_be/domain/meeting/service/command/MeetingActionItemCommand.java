package org.wonik.coreviaai_be.domain.meeting.service.command;

import java.time.LocalDate;

public record MeetingActionItemCommand(String content, Long assigneeUserId, LocalDate dueDate) {
}
