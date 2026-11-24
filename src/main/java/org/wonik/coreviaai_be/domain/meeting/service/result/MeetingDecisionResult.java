package org.wonik.coreviaai_be.domain.meeting.service.result;

import org.wonik.coreviaai_be.domain.meeting.entity.type.MeetingItemSource;

public record MeetingDecisionResult(Long id, String content, int sortOrder, MeetingItemSource source) {
}
