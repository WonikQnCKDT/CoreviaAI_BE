package org.wonik.coreviaai_be.domain.aidraft.entity.type;

/**
 * AI 결과의 처리 상태(NFR-08 사람 승인). DRAFT에서 시작해 사람이 수락·거절하면 바뀐다. 생성 실패는 FAILED로 끝난다.
 */
public enum AiDraftStatus {
	DRAFT,
	ACCEPTED,
	PARTIALLY_ACCEPTED,
	REJECTED,
	FAILED
}
