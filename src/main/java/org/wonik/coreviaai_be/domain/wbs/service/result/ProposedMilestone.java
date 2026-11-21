package org.wonik.coreviaai_be.domain.wbs.service.result;

import java.time.LocalDate;
import java.util.List;

/**
 * 제안 마일스톤.
 *
 * @param relatedTaskIds 연결 작업의 tempId
 */
public record ProposedMilestone(
	String tempId,
	String name,
	LocalDate targetDate,
	List<String> relatedTaskIds,
	String reason
) {
}
