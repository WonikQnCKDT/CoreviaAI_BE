package org.wonik.coreviaai_be.domain.aidraft.service;

import java.time.Clock;
import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.aidraft.entity.AiDraft;
import org.wonik.coreviaai_be.domain.aidraft.entity.type.AiDraftFeature;
import org.wonik.coreviaai_be.domain.aidraft.entity.type.AiDraftStatus;
import org.wonik.coreviaai_be.domain.aidraft.repository.AiDraftRepository;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

/**
 * AI 결과와 사람의 결정을 ai_drafts에 남긴다. 각 AI 기능(wbs 등)이 호출하며, 기능 로직은 이 클래스에 두지 않는다.
 * <ul>
 *   <li>생성 기록(recordDraft·recordFailure): AI 호출이 끝난 뒤 자체 트랜잭션으로 저장한다(AI 호출 중 DB 커넥션을 잡지 않음)</li>
 *   <li>결정 기록(decide): 업무 데이터 반영과 같은 트랜잭션에서만 호출한다(반영과 기록이 함께 커밋·롤백)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AiDraftRecorder {

	private final AiDraftRepository repository;
	private final JsonMapper jsonMapper;
	private final Clock clock;

	/** AI 결과를 DRAFT로 저장하고 ID를 돌려준다. output은 JSON으로 저장한다. */
	@Transactional
	public Long recordDraft(
		final AiDraftFeature feature,
		final Long projectId,
		final Long requestedBy,
		final Object output,
		final Duration latency
	) {
		final AiDraft draft = AiDraft.draft(feature, projectId, requestedBy, jsonMapper.writeValueAsString(output),
			toMillis(latency));
		return repository.save(draft).getId();
	}

	/** AI 생성 실패를 FAILED로 저장한다. */
	@Transactional
	public Long recordFailure(
		final AiDraftFeature feature,
		final Long projectId,
		final Long requestedBy,
		final Duration latency
	) {
		return repository.save(AiDraft.failed(feature, projectId, requestedBy, toMillis(latency))).getId();
	}

	/**
	 * 사람의 결정을 남긴다. 다른 기능·프로젝트·요청자의 초안이거나 없으면 404 AI_DRAFT_EXPIRED,
	 * 이미 결정된 초안이면 409 AI_DRAFT_ALREADY_APPLIED.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public void decide(
		final Long aiDraftId,
		final AiDraftFeature feature,
		final Long projectId,
		final Long decidedBy,
		final AiDraftStatus decision
	) {
		final AiDraft draft = repository.findById(aiDraftId)
			.filter(found -> found.getFeature() == feature)
			.filter(found -> projectId.equals(found.getProjectId()))
			.filter(found -> decidedBy.equals(found.getRequestedBy()))
			.orElseThrow(() -> new CustomException(ErrorCode.AI_DRAFT_EXPIRED));
		draft.decide(decision, decidedBy, clock.instant());
	}

	private static int toMillis(final Duration latency) {
		return (int) Math.min(Integer.MAX_VALUE, Math.max(0, latency.toMillis()));
	}
}
