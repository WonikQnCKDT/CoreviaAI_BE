package org.wonik.aiserver.domain.wbs.api;

/**
 * 프로젝트에 저장된 단계(existingStages). AI는 새 단계를 만들지 않고 phaseId를 그대로 돌려준다.
 */
public record AiWbsStage(Long phaseId, String name, int order) {
}
