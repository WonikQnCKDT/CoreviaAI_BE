package org.wonik.coreviaai_be.domain.wbs.entity;

import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskPriority;
import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskType;

/**
 * 업무 상세 필드(FR-TSK-01): 유형·우선순위·완료 조건. 담당자·기간·예상 공수 같은 기본 필드는 {@link TaskDetails}.
 * 검증(길이)이 끝난 값만 넘긴다.
 *
 * @param completionCriteria 완료 조건. 없으면 null
 */
public record TaskAttributes(TaskType taskType, TaskPriority priority, String completionCriteria) {

	/** 새 업무 기본값(ERD 기본값과 같음): 작업, 보통, 완료 조건 없음. */
	public static final TaskAttributes DEFAULT = new TaskAttributes(TaskType.TASK, TaskPriority.MEDIUM, null);

	public TaskAttributes {
		if (taskType == null || priority == null) {
			throw new IllegalArgumentException("업무 유형과 우선순위는 비울 수 없습니다");
		}
	}
}
