package org.wonik.coreviaai_be.domain.meeting.dto.response;

import org.wonik.coreviaai_be.domain.meeting.entity.type.MeetingItemSource;
import org.wonik.coreviaai_be.domain.meeting.service.result.MeetingDecisionResult;

public record MeetingDecisionResponse(Long id, String content, int sortOrder, MeetingItemSource source) {
	public static MeetingDecisionResponse from(final MeetingDecisionResult result) {
		return new MeetingDecisionResponse(result.id(), result.content(), result.sortOrder(), result.source());
	}
}
