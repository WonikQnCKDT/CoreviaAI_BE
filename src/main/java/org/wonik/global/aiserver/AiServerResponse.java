package org.wonik.global.aiserver;

/**
 * AI 서버 응답 봉투. 공통 규약(00. 공통 규약 2절)과 같은 { success, response, error } 구조다.
 *
 * @param <T> AI 생성 결과(result) 타입
 */
public record AiServerResponse<T>(boolean success, AiServerPayload<T> response, AiServerError error) {
}
