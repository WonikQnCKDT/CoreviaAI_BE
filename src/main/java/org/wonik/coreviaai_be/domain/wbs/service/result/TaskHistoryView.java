package org.wonik.coreviaai_be.domain.wbs.service.result;

import org.wonik.coreviaai_be.domain.wbs.entity.TaskHistory;

import tools.jackson.databind.JsonNode;

/**
 * 업무 이력 한 건과 표시용 값.
 *
 * @param before    변경 전 값(JSON 객체). 없으면 null
 * @param after     변경 후 값(JSON 객체). 없으면 null
 * @param actorName 수행자 이름. 시스템 처리이거나 찾지 못하면 null
 */
public record TaskHistoryView(TaskHistory history, JsonNode before, JsonNode after, String actorName) {
}
