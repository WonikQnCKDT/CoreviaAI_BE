package org.wonik.coreviaai_be.domain.wbs.drafttoken;

/**
 * 초안 토큰을 발급받은 프로젝트·PM과 그 초안 기록(ai_drafts.id). 다른 프로젝트나 다른 사용자는 이 토큰으로 반영할 수 없다.
 */
public record DraftTokenOwner(Long projectId, Long userId, Long aiDraftId) {

	public boolean isOwnedBy(final Long projectId, final Long userId) {
		return this.projectId.equals(projectId) && this.userId.equals(userId);
	}
}
