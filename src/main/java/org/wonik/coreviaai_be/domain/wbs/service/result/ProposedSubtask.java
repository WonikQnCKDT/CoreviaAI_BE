package org.wonik.coreviaai_be.domain.wbs.service.result;

import java.math.BigDecimal;

/**
 * 제안 하위작업.
 */
public record ProposedSubtask(String tempId, String name, String description, BigDecimal estimatedPersonDays) {
}
