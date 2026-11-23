package org.wonik.coreviaai_be.domain.wbs.service.command;

import org.wonik.coreviaai_be.domain.wbs.entity.TaskAttributes;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskDetails;

/**
 * 작업·하위작업 직접 등록(FR-WBS-01). 업무 상세 필드(FR-TSK-01)는 등록 때 함께 정할 수 있다.
 *
 * @param parentTaskId 상위 작업 ID. null이면 최상위 작업
 * @param attributes   유형·우선순위·완료 조건. 요청에서 비운 값은 기본값(TASK·MEDIUM·없음)
 */
public record CreateWbsTaskCommand(Long phaseId, Long parentTaskId, TaskDetails details, TaskAttributes attributes) {

	public CreateWbsTaskCommand {
		if (attributes == null) {
			attributes = TaskAttributes.DEFAULT;
		}
	}
}
