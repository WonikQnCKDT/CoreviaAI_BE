package org.wonik.coreviaai_be.domain.wbs.drafttoken;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI WBS 초안 설정(app.wbs.ai-draft.*).
 *
 * @param tokenTtl   초안을 반영할 수 있는 시간. PM이 검토하는 동안 만료되지 않을 만큼 둔다
 * @param tokenStore 토큰 저장소 종류(redis | memory). memory는 테스트 전용
 */
@ConfigurationProperties(prefix = "app.wbs.ai-draft")
public record WbsAiDraftProperties(Duration tokenTtl, String tokenStore) {
}
