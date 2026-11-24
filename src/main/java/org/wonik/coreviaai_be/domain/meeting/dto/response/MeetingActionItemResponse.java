package org.wonik.coreviaai_be.domain.meeting.dto.response;

import java.time.LocalDate;

import org.wonik.coreviaai_be.domain.meeting.entity.type.ActionItemStatus;
import org.wonik.coreviaai_be.domain.meeting.entity.type.MeetingItemSource;
import org.wonik.coreviaai_be.domain.meeting.service.result.MeetingActionItemResult;

public record MeetingActionItemResponse(
	Long id,
	String content,
	Long assigneeUserId,
	String assigneeName,
	LocalDate dueDate,
	ActionItemStatus status,
	MeetingItemSource source,
	Long taskId
) {
	public static MeetingActionItemResponse from(final MeetingActionItemResult result) {
		return new MeetingActionItemResponse(result.id(), result.content(), result.assigneeUserId(), result.assigneeName(),
			result.dueDate(), result.status(), result.source(), result.taskId());
	}
}
