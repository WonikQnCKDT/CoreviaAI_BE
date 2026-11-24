package org.wonik.coreviaai_be.domain.meeting.dto.request;

import java.time.LocalDate;

import org.wonik.coreviaai_be.domain.meeting.entity.ActionItem;
import org.wonik.coreviaai_be.domain.meeting.service.command.MeetingActionItemCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MeetingActionItemRequest(
	@NotBlank @Size(max = ActionItem.CONTENT_MAX_LENGTH) String content,
	@Positive Long assigneeUserId,
	LocalDate dueDate
) {
	public MeetingActionItemCommand toCommand() {
		return new MeetingActionItemCommand(content, assigneeUserId, dueDate);
	}
}
