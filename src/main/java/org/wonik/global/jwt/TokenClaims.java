package org.wonik.global.jwt;

/**
 * 검증을 통과한 토큰에서 꺼낸 값. 권한 판단은 이 값이 아니라 세션 저장소의 세션 기준으로 한다.
 */
public record TokenClaims(Long userId, String sessionId) {
}
