package org.wonik.global.aiserver;

/**
 * AI 서버 성공 응답의 response 부분(02. AI 생성 API 공통 사항).
 *
 * @param status AI 결과 상태. 생성 API는 항상 AI_DRAFT(승인·확정 상태는 BE가 관리)
 * @param result 기능별 생성 결과
 */
public record AiServerPayload<T>(String status, T result) {
}
