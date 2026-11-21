package org.wonik.coreviaai_be.domain.wbs.service.command;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 승인한 하위작업 한 개. AI 초안의 하위작업에는 기간이 없어, 비워 두면 상위 작업 기간을 쓴다.
 */
public record DraftSubtaskCommand(
	String tempId,
	String name,
	String description,
	LocalDate startDate,
	LocalDate endDate,
	BigDecimal estimatedPersonDays,
	Long assigneeUserId
) {

	public LocalDate startDateOr(final LocalDate parentStartDate) {
		return startDate == null ? parentStartDate : startDate;
	}

	public LocalDate endDateOr(final LocalDate parentEndDate) {
		return endDate == null ? parentEndDate : endDate;
	}
}
