package org.wonik.coreviaai_be.domain.wbs.dto.response;

import java.time.Instant;
import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.service.result.DraftFeasibility;
import org.wonik.coreviaai_be.domain.wbs.service.result.GeneratedWbsDraft;
import org.wonik.coreviaai_be.domain.wbs.service.result.ProposedMilestone;
import org.wonik.coreviaai_be.domain.wbs.service.result.ProposedStage;
import org.wonik.coreviaai_be.domain.wbs.service.result.WbsDraftProposal;

/**
 * AI WBS 초안 응답. status는 항상 AI_DRAFT로, 화면에 'AI 초안' 표시를 하기 위한 값이다(NFR-10).
 * 제안 항목은 서비스 결과(ProposedStage 등)를 그대로 내보낸다. 결과 자체가 저장하지 않는 표시용 값이라
 * 같은 모양의 응답 타입을 한 벌 더 두지 않는다.
 *
 * @param draftToken 반영 API에 그대로 보내는 토큰. expiresAt(UTC)이 지나면 반영할 수 없다
 */
public record WbsDraftResponse(
	String status,
	String draftToken,
	Instant expiresAt,
	List<ProposedStage> stages,
	List<ProposedMilestone> milestones,
	DraftFeasibility feasibility,
	List<String> warnings,
	List<String> suggestions,
	List<String> assumptions,
	List<String> clarificationQuestions
) {

	public static final String AI_DRAFT = "AI_DRAFT";

	public static WbsDraftResponse from(final GeneratedWbsDraft draft) {
		final WbsDraftProposal proposal = draft.proposal();
		return new WbsDraftResponse(
			AI_DRAFT,
			draft.draftToken(),
			draft.expiresAt(),
			proposal.stages(),
			proposal.milestones(),
			proposal.feasibility(),
			proposal.warnings(),
			proposal.suggestions(),
			proposal.assumptions(),
			proposal.clarificationQuestions()
		);
	}
}
