package org.wonik.coreviaai_be.domain.wbs.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 작업·하위작업을 만들 때 받는 내용. 검증(기간·담당자·길이)이 끝난 값만 넘긴다.
 *
 * @param assigneeUserId  담당자(프로젝트 멤버). 미지정이면 null
 * @param estimatedEffort 예상 공수(인일). 미입력이면 null
 */
public record TaskDetails(
	String name,
	String description,
	Long assigneeUserId,
	LocalDate startDate,
	LocalDate endDate,
	BigDecimal estimatedEffort
) {
}
