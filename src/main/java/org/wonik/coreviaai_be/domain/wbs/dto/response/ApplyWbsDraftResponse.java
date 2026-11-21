package org.wonik.coreviaai_be.domain.wbs.dto.response;

import java.util.Map;

import org.wonik.coreviaai_be.domain.wbs.service.result.AppliedWbsDraft;

/**
 * AI 초안 반영 응답: 초안 tempId → 새로 발급한 정식 ID.
 *
 * @param taskIds      작업·하위작업
 * @param milestoneIds 마일스톤
 */
public record ApplyWbsDraftResponse(Map<String, Long> taskIds, Map<String, Long> milestoneIds) {

	public static ApplyWbsDraftResponse from(final AppliedWbsDraft applied) {
		return new ApplyWbsDraftResponse(applied.taskIds(), applied.milestoneIds());
	}
}
