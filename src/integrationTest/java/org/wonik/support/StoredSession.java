package org.wonik.support;

import java.time.Instant;

import org.wonik.authserver.domain.auth.session.AuthSession;

/**
 * 메모리 세션 저장소의 항목: 세션과 무활동 만료 시각.
 */
public record StoredSession(AuthSession session, Instant expiresAt) {
}
