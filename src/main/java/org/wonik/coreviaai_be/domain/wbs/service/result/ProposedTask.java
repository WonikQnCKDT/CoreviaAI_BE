package org.wonik.coreviaai_be.domain.wbs.service.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 제안 작업.
 *
 * @param suggestedAssigneeId 제안 담당자(멤버 중). 판단할 수 없으면 null
 * @param suggestedRole       제안 역할(ProjectActor 값). 없으면 null
 * @param dependencies        선행 작업의 tempId
 * @param reason              제안 근거
 */
public record ProposedTask(
	String tempId,
	String name,
	String description,
	BigDecimal estimatedPersonDays,
	Long suggestedAssigneeId,
	String suggestedRole,
	LocalDate startDate,
	LocalDate endDate,
	List<String> dependencies,
	List<ProposedSubtask> subtasks,
	String reason
) {
}
