package org.wonik.aiserver.domain.wbs.api;

import java.time.LocalDate;
import java.util.List;

/**
 * 제안 마일스톤. relatedTaskIds는 결과 안 작업의 tempId다.
 */
public record AiWbsDraftMilestone(
	String tempId,
	String name,
	LocalDate targetDate,
	List<String> relatedTaskIds,
	String reason
) {
}
