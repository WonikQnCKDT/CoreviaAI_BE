package org.wonik.coreviaai_be.domain.aidraft.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.aidraft.entity.type.AiDraftFeature;
import org.wonik.coreviaai_be.domain.aidraft.entity.type.AiDraftStatus;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * ai_drafts 상태 전이: DRAFT에서 사람의 결정으로 한 번만 바뀐다(NFR-08).
 */
class AiDraftTest {

	private static final Instant NOW = Instant.parse("2026-11-21T00:00:00Z");

	@Test
	@DisplayName("DRAFT는 수락·일부 수락·거절로 바뀌고 결정자·시각을 남긴다")
	void decide() {
		final AiDraft draft = AiDraft.draft(AiDraftFeature.WBS_DRAFT, 1L, 2L, "{}", 10);

		draft.decide(AiDraftStatus.PARTIALLY_ACCEPTED, 2L, NOW);

		assertThat(draft.getStatus()).isEqualTo(AiDraftStatus.PARTIALLY_ACCEPTED);
		assertThat(draft.getDecidedBy()).isEqualTo(2L);
		assertThat(draft.getDecidedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("이미 결정된 초안이나 실패 기록은 다시 결정할 수 없다(409)")
	void decideOnce() {
		final AiDraft decided = AiDraft.draft(AiDraftFeature.WBS_DRAFT, 1L, 2L, "{}", 10);
		decided.decide(AiDraftStatus.ACCEPTED, 2L, NOW);
		final AiDraft failed = AiDraft.failed(AiDraftFeature.WBS_DRAFT, 1L, 2L, 10);

		assertThatThrownBy(() -> decided.decide(AiDraftStatus.REJECTED, 2L, NOW))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.AI_DRAFT_ALREADY_APPLIED);
		assertThatThrownBy(() -> failed.decide(AiDraftStatus.ACCEPTED, 2L, NOW))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.AI_DRAFT_ALREADY_APPLIED);
	}

	@Test
	@DisplayName("결정 값으로 DRAFT·FAILED는 받지 않는다")
	void invalidDecision() {
		final AiDraft draft = AiDraft.draft(AiDraftFeature.WBS_DRAFT, 1L, 2L, "{}", 10);

		assertThatThrownBy(() -> draft.decide(AiDraftStatus.FAILED, 2L, NOW))
			.isInstanceOf(IllegalArgumentException.class);
		assertThat(draft.getStatus()).isEqualTo(AiDraftStatus.DRAFT);
	}
}
