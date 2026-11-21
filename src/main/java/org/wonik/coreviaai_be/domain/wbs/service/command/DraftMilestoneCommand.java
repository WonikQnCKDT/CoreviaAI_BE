package org.wonik.coreviaai_be.domain.wbs.service.command;

import java.time.LocalDate;
import java.util.List;

/**
 * 승인한 마일스톤 한 개.
 *
 * @param relatedTaskIds 연결할 작업의 tempId(같은 요청 안의 작업만)
 */
public record DraftMilestoneCommand(String tempId, String name, LocalDate targetDate, List<String> relatedTaskIds) {
}
