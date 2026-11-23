package org.wonik.aiserver.domain.wbs.api;

import java.util.List;

/**
 * 단계별 제안 작업. phaseId는 요청한 existingStages 중 하나다.
 */
public record AiWbsDraftStage(Long phaseId, List<AiWbsDraftTask> tasks) {
}
