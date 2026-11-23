package org.wonik.coreviaai_be.domain.worklog.dto.response;

import java.time.Instant;
import java.time.LocalDate;

import org.wonik.coreviaai_be.domain.worklog.service.result.WorklogResult;

public record WorklogResponse(LocalDate logDate, String content, Instant createdAt, Instant updatedAt) {

	public static WorklogResponse from(final WorklogResult result) {
		return new WorklogResponse(result.logDate(), result.content(), result.createdAt(), result.updatedAt());
	}
}
