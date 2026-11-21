package org.wonik.support;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.wbs.drafttoken.DraftTokenOwner;

/**
 * 메모리 초안 토큰 저장소의 한 항목과 만료 시각.
 */
record StoredToken(DraftTokenOwner owner, Instant expiresAt) {
}
