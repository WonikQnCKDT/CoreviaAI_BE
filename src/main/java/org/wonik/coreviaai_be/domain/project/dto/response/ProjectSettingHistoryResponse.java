package org.wonik.coreviaai_be.domain.project.dto.response;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.project.entity.type.ProjectSettingChangeType;
import org.wonik.coreviaai_be.domain.project.service.result.ProjectSettingHistoryResult;

import tools.jackson.databind.JsonNode;

public record ProjectSettingHistoryResponse(
	Long id,
	ProjectSettingChangeType changeType,
	JsonNode before,
	JsonNode after,
	Long changedBy,
	String changedByName,
	Instant occurredAt
) {

	public static ProjectSettingHistoryResponse from(final ProjectSettingHistoryResult result) {
		return new ProjectSettingHistoryResponse(
			result.history().getId(),
			result.history().getChangeType(),
			result.before(),
			result.after(),
			result.history().getChangedBy(),
			result.changedByName(),
			result.history().getOccurredAt()
		);
	}
}
