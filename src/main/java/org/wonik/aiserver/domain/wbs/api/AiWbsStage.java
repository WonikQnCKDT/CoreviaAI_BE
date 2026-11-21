package org.wonik.aiserver.domain.wbs.api;

/**
 * 프로젝트 생성 때 만든 단계(existingStages). stageCode는 WaterfallPhaseType 값이다. AI는 새 단계를 만들지 않는다.
 */
public record AiWbsStage(String stageCode, String name, int order) {
}
