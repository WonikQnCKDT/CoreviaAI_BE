package org.wonik.coreviaai_be.domain.wbs.service.command;

import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

/**
 * AI 초안 생성 시 알려 주는 기존 작업(중복 제안 방지용). 작업의 단계는 저장하지 않아 보내지 않는다.
 */
public record DraftExistingTask(Long taskId, String name, WbsTaskStatus status) {
}
