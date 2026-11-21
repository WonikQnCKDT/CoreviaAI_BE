package org.wonik.aiserver.domain.wbs.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 제안 작업. tempId는 임시 ID(정식 ID는 PM 승인 후 업무 모듈이 발급), dependencies는 선행 작업의 tempId다(완료→시작).
 */
public record AiWbsDraftTask(
	String tempId,
	String name,
	String description,
	BigDecimal estimatedPersonDays,
	Long suggestedAssigneeId,
	String suggestedRole,
	LocalDate startDate,
	LocalDate endDate,
	List<String> dependencies,
	List<AiWbsDraftSubtask> subtasks,
	String reason
) {
}
