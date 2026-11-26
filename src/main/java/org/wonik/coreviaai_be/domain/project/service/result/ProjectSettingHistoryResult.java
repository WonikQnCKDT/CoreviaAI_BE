package org.wonik.coreviaai_be.domain.project.service.result;

import org.wonik.coreviaai_be.domain.project.entity.ProjectSettingHistory;

import tools.jackson.databind.JsonNode;

public record ProjectSettingHistoryResult(
	ProjectSettingHistory history,
	String changedByName,
	JsonNode before,
	JsonNode after
) {
}
