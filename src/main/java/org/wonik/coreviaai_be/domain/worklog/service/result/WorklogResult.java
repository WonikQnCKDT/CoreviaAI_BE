package org.wonik.coreviaai_be.domain.worklog.service.result;

import java.time.Instant;
import java.time.LocalDate;

import org.wonik.coreviaai_be.domain.worklog.entity.Worklog;

public record WorklogResult(LocalDate logDate, String content, Instant createdAt, Instant updatedAt) {

	public static WorklogResult from(final Worklog worklog) {
		return new WorklogResult(worklog.getLogDate(), worklog.getContent(), worklog.getCreatedAt(),
			worklog.getUpdatedAt());
	}
}
