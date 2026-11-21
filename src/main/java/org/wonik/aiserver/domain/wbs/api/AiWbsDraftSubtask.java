package org.wonik.aiserver.domain.wbs.api;

import java.math.BigDecimal;

/**
 * 제안 하위작업. AI 명세상 기간이 없어 반영할 때 상위 작업 기간을 기본값으로 쓴다.
 */
public record AiWbsDraftSubtask(String tempId, String name, String description, BigDecimal estimatedPersonDays) {
}
