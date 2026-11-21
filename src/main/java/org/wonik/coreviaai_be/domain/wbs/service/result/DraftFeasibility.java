package org.wonik.coreviaai_be.domain.wbs.service.result;

/**
 * 일정 실현 가능성. status는 AI 서버 값(FEASIBLE·AT_RISK·INFEASIBLE·INSUFFICIENT_INFO)을 그대로 전달한다.
 */
public record DraftFeasibility(String status, String reason) {
}
