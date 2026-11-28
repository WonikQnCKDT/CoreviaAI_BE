package org.wonik.coreviaai_be.domain.wbs.service.result;

import java.time.LocalDate;

import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

/**
 * 마일스톤에 연결한 작업 한 건(milestone_tasks + wbs_tasks 조인 결과). 지연 경고 계산과 상세 표시에 쓴다.
 *
 * @param parentTaskId 하위작업이면 상위 작업 ID, 작업이면 null
 */
public record MilestoneLinkedTask(
	Long milestoneId,
	Long taskId,
	Long parentTaskId,
	String name,
	WbsTaskStatus status,
	LocalDate endDate
) {

	/** 하위작업 기준 파생 상태를 반영한 사본을 돌려준다. 상태가 같으면 그대로 반환한다. */
	public MilestoneLinkedTask withStatus(final WbsTaskStatus effectiveStatus) {
		if (status == effectiveStatus) {
			return this;
		}
		return new MilestoneLinkedTask(milestoneId, taskId, parentTaskId, name, effectiveStatus, endDate);
	}
}
