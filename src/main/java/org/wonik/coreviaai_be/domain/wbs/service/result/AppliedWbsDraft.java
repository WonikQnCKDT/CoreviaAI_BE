package org.wonik.coreviaai_be.domain.wbs.service.result;

import java.util.Map;

/**
 * 반영 결과: 초안 tempId → 새로 발급한 정식 ID.
 *
 * @param taskIds      작업·하위작업
 * @param milestoneIds 마일스톤
 */
public record AppliedWbsDraft(Map<String, Long> taskIds, Map<String, Long> milestoneIds) {
}
