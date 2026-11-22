package org.wonik.coreviaai_be.domain.wbs.entity.type;

/**
 * 업무 수행 이력 종류(task_histories.activity_type, 05. 업무·보드 ERD). 기능이 늘면 값을 추가한다(DB CHECK 없음).
 * before_value·after_value에 담는 필드는 {@link org.wonik.coreviaai_be.domain.wbs.service.TaskHistoryRecorder}가 정한다.
 */
public enum TaskActivityType {
	/** 등록(직접 등록·AI 초안 반영). after = 등록 값 */
	CREATED,
	/** 담당자 배정·변경. before·after = assigneeUserId(before는 null일 수 있음) */
	ASSIGNED,
	/** 담당자 해제. before = assigneeUserId, after = assigneeUserId(null) */
	UNASSIGNED,
	/** 시작·종료일 변경(직접 수정, 하위작업 변경에 따른 상위 기간 산출 포함). before·after = startDate, endDate */
	DATE_CHANGED,
	/** 이름·설명·예상 공수·유형·우선순위·완료 조건 변경. 바뀐 필드만 before·after에 담는다 */
	DETAILS_CHANGED,
	/** 계층·순서 이동(드래그). before·after = parentTaskId, sortOrder */
	MOVED,
	/** 삭제. before = 삭제 직전 값. 작업 행이 지워진 뒤에도 이력은 남는다 */
	DELETED,
	/** 상태 변경(칸반 카드 이동, FR-TSK-02). before·after = status, boardColumnId */
	STATUS_CHANGED,
	/** 같은 상태의 다른 칸반 컬럼으로 이동(상태는 그대로). before·after = boardColumnId */
	BOARD_COLUMN_CHANGED,
	/** 진척률 변경(완료 전환 시 100 자동 반영 등, FR-TSK-04). before·after = progress */
	PROGRESS_CHANGED
}
