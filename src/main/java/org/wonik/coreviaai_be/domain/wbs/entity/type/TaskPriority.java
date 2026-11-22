package org.wonik.coreviaai_be.domain.wbs.entity.type;

/**
 * 업무 우선순위(FR-TSK-01). 새 업무는 MEDIUM으로 시작한다. 값은 DB CHECK(ck_wbs_tasks_priority)와 같다.
 */
public enum TaskPriority {
	HIGH,
	MEDIUM,
	LOW
}
