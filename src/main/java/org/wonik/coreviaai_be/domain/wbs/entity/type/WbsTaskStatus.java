package org.wonik.coreviaai_be.domain.wbs.entity.type;

import java.util.Collection;

/**
 * 작업 상태. 칸반 기본 컬럼(FR-TSK-02: 대기·진행·검토·완료)과 같은 순서다. 새 작업은 TODO로 시작한다.
 */
public enum WbsTaskStatus {
	TODO,
	IN_PROGRESS,
	REVIEW,
	DONE;

	/**
	 * 하위작업이 있는 상위 작업의 유효 상태를 하위작업 상태로부터 파생한다(FE {@code wbsRules.deriveDisplayStatus}와 같은 규칙).
	 * <ul>
	 *   <li>하위작업이 없으면 저장된 자신의 상태 그대로</li>
	 *   <li>하위작업이 모두 DONE → DONE</li>
	 *   <li>하위작업이 모두 TODO → TODO</li>
	 *   <li>하위작업이 모두 REVIEW 또는 DONE → REVIEW</li>
	 *   <li>그 외 → IN_PROGRESS</li>
	 * </ul>
	 */
	public static WbsTaskStatus deriveFromSubtasks(
		final WbsTaskStatus storedStatus,
		final Collection<WbsTaskStatus> subtaskStatuses
	) {
		if (subtaskStatuses == null || subtaskStatuses.isEmpty()) {
			return storedStatus;
		}
		boolean allDone = true;
		boolean allTodo = true;
		boolean allReviewOrDone = true;
		for (final WbsTaskStatus status : subtaskStatuses) {
			if (status != DONE) {
				allDone = false;
			}
			if (status != TODO) {
				allTodo = false;
			}
			if (status != REVIEW && status != DONE) {
				allReviewOrDone = false;
			}
		}
		if (allDone) {
			return DONE;
		}
		if (allTodo) {
			return TODO;
		}
		if (allReviewOrDone) {
			return REVIEW;
		}
		return IN_PROGRESS;
	}
}
