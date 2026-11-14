package org.wonik.global.jwt;

import org.wonik.global.common.type.SystemRole;

/**
 * 인증된 요청의 principal. 컨트롤러에서 {@code @AuthenticationPrincipal LoginUser}로 받는다.
 * <p>
 * 업무 모듈은 인증 방식(authserver 로컬 로그인, 이후 사내 SSO)을 모른 채 이 타입만 쓴다.
 */
public record LoginUser(Long userId, String sessionId, SystemRole systemRole) {
}
