package org.wonik.coreviaai_be.domain.wbs.client.dto;

/**
 * 일정 실현 가능성 판단. status는 FEASIBLE·AT_RISK·INFEASIBLE·INSUFFICIENT_INFO 중 하나다.
 */
public record AiWbsFeasibility(String status, String reason) {
}
