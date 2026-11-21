package org.wonik.coreviaai_be.domain.wbs.service.command;

import org.wonik.coreviaai_be.domain.wbs.entity.TaskDetails;

/**
 * 작업·하위작업 직접 등록(FR-WBS-01).
 *
 * @param parentTaskId 상위 작업 ID. null이면 최상위 작업
 */
public record CreateWbsTaskCommand(Long parentTaskId, TaskDetails details) {
}
