package org.wonik.coreviaai_be.domain.wbs.service.result;

import java.time.Instant;

/**
 * 초안 생성 결과. draftToken은 반영 API를 한 번만 허용하기 위한 값이며 초안 내용은 서버에 남지 않는다.
 *
 * @param expiresAt 이 시각 이후에는 반영할 수 없다(UTC)
 */
public record GeneratedWbsDraft(String draftToken, Instant expiresAt, WbsDraftProposal proposal) {
}
