package org.wonik.coreviaai_be.domain.wbs.dto.response;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.wbs.entity.TaskHistory;
import org.wonik.coreviaai_be.domain.wbs.service.result.TaskHistoryView;

import tools.jackson.databind.JsonNode;

/**
 * 업무 이력 한 건(FR-TSK-01 변경 이력, FR-TSK-06 타임라인 원천). before·after는 activityType별로 정해진 필드의 JSON 객체다.
 * 문자열 값(작업명·설명 등)은 사용자 입력이므로 화면은 텍스트로만 표시한다(HTML로 해석하지 않음).
 *
 * @param before 변경 전 값. 등록(CREATED)은 null
 * @param after  변경 후 값. 삭제(DELETED)는 null
 * @param actor  수행자. 시스템 처리는 null
 */
public record TaskHistoryResponse(
	Long id,
	String activityType,
	JsonNode before,
	JsonNode after,
	TaskDetailResponse.UserSummary actor,
	Instant occurredAt
) {

	public static TaskHistoryResponse from(final TaskHistoryView view) {
		final TaskHistory history = view.history();
		final TaskDetailResponse.UserSummary actor = history.getActorUserId() == null
			? null
			: new TaskDetailResponse.UserSummary(history.getActorUserId(), view.actorName());
		return new TaskHistoryResponse(history.getId(), history.getActivityType().name(), view.before(), view.after(),
			actor, history.getOccurredAt());
	}
}
