package org.wonik.coreviaai_be.domain.wbs.client.dto;

import java.util.List;

/**
 * 단계별 제안 작업. stageCode는 요청한 existingStages 중 하나다.
 */
public record AiWbsDraftStage(String stageCode, List<AiWbsDraftTask> tasks) {
}
