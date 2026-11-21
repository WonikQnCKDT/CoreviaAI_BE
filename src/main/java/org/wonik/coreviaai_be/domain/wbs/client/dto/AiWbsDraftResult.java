package org.wonik.coreviaai_be.domain.wbs.client.dto;

import java.util.List;

/**
 * AI 서버 POST /generate/wbs 응답의 result(WbsDraftResult). AI 서버가 tempId 중복·순환·기간을 검증한 결과지만,
 * PM이 수정한 값으로 반영하므로 BE는 반영 시점에 다시 검증한다.
 */
public record AiWbsDraftResult(
	Long projectId,
	List<AiWbsDraftStage> stages,
	List<AiWbsDraftMilestone> milestones,
	AiWbsFeasibility feasibility,
	List<String> warnings,
	List<String> suggestions,
	List<String> assumptions,
	List<String> clarificationQuestions
) {
}
