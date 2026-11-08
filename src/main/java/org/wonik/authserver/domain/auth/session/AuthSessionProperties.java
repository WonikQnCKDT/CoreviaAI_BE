package org.wonik.authserver.domain.auth.session;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 로그인 세션 설정(app.auth.*).
 *
 * @param idleTimeout  무활동 세션 만료 시간(NFR-04: 30분)
 * @param sessionStore 세션 저장소 종류(redis | memory). memory는 테스트 전용
 */
@ConfigurationProperties(prefix = "app.auth")
public record AuthSessionProperties(Duration idleTimeout, String sessionStore) {
}
