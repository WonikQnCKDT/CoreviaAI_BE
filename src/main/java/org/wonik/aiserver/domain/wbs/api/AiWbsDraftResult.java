package org.wonik.aiserver.domain.wbs.api;

import java.util.List;

/**
 * WBS 초안 생성 결과(02. AI 생성 API의 WbsDraftResult). aiserver가 tempId 중복·순환·기간을 검증한 결과지만,
 * PM이 수정한 값으로 반영하므로 업무 모듈은 반영 시점에 다시 검증한다.
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
