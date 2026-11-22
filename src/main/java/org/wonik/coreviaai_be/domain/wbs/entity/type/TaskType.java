package org.wonik.coreviaai_be.domain.wbs.entity.type;

/**
 * 업무 유형(FR-TSK-01). 새 업무는 TASK로 시작한다. 값은 DB CHECK(ck_wbs_tasks_task_type)와 같다.
 */
public enum TaskType {
	/** 작업 */
	TASK,
	/** 결함 */
	DEFECT
}
