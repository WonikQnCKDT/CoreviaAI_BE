package org.wonik.coreviaai_be.domain.wbs.service.command;

import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

/**
 * AI 초안 생성 시 알려 주는 기존 작업(중복 제안 방지용).
 */
public record DraftExistingTask(Long taskId, Long phaseId, String name, WbsTaskStatus status) {
	public DraftExistingTask(final Long taskId, final String name, final WbsTaskStatus status) {
		this(taskId, null, name, status);
	}
}
