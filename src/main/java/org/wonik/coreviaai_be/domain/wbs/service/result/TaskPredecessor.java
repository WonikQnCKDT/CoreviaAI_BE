package org.wonik.coreviaai_be.domain.wbs.service.result;

import java.time.LocalDate;

import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

/** 선행 작업과 현재 날짜 기준 지연·후행 영향 정보. */
public record TaskPredecessor(
	Long id,
	Long parentTaskId,
	String name,
	WbsTaskStatus status,
	LocalDate endDate,
	boolean delayed,
	long delayDays,
	boolean impactsSuccessor
) {
}
