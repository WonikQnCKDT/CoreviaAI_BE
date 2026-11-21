package org.wonik.global.aiserver;

/**
 * AI 서버 실패 응답의 error 부분. 메시지는 운영 진단용으로만 쓰고 사용자 응답에 그대로 노출하지 않는다.
 */
public record AiServerError(String message, int status) {
}
