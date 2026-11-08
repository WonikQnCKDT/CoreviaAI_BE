package org.wonik.global.security;

import org.wonik.global.jwt.LoginUser;

/**
 * 토큰이 가리키는 살아 있는 로그인 세션.
 *
 * @param user               요청 principal
 * @param mustChangePassword 비밀번호 변경 전까지 허용 경로 외 요청을 막아야 하는지
 */
public record LoginSession(LoginUser user, boolean mustChangePassword) {
}
