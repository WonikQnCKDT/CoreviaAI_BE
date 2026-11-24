package org.wonik.coreviaai_be.domain.meeting.service.result;

import java.time.LocalDate;

import org.wonik.coreviaai_be.domain.meeting.entity.type.ActionItemStatus;
import org.wonik.coreviaai_be.domain.meeting.entity.type.MeetingItemSource;

public record MeetingActionItemResult(
	Long id,
	String content,
	Long assigneeUserId,
	String assigneeName,
	LocalDate dueDate,
	ActionItemStatus status,
	MeetingItemSource source,
	Long taskId
) {
}
