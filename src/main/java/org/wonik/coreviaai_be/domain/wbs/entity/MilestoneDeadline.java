package org.wonik.coreviaai_be.domain.wbs.entity;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;

import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

/**
 * 마일스톤의 D-day와 지연 경고(FR-WBS-04). 조회할 때마다 규칙으로 계산하고 저장하지 않는다(명세서 2장: 감지는 규칙 기반).
 * <ul>
 *   <li>dDay: 오늘부터 목표일까지 남은 일수. 양수는 남은 날(D-3), 0은 당일(D-day), 음수는 지난 날(D+2)</li>
 *   <li>지연 경고: 목표일이 도래했고(오늘 ≥ 목표일, 당일 포함) 연결 작업 중 완료(DONE)가 아닌 것이 하나라도 있을 때.
 *       인수 기준 '연결 작업 미완료 상태로 기한 도래 시'의 '도래'를 당일 포함으로 본다</li>
 *   <li>연결 작업이 없으면 판단 근거가 없어 경고하지 않는다(linkedTaskCount 0으로 화면이 '연결 작업 없음'을 표시)</li>
 * </ul>
 *
 * @param linkedTaskCount     연결 작업 수
 * @param incompleteTaskCount 연결 작업 중 DONE이 아닌 수(경고 여부와 관계없이 항상 계산)
 */
public record MilestoneDeadline(long dDay, boolean delayed, int linkedTaskCount, int incompleteTaskCount) {

	/**
	 * @param today              업무 시간대 기준 오늘(BusinessCalendar)
	 * @param linkedTaskStatuses 연결 작업들의 현재 상태
	 */
	public static MilestoneDeadline evaluate(
		final LocalDate targetDate,
		final LocalDate today,
		final Collection<WbsTaskStatus> linkedTaskStatuses
	) {
		final long dDay = ChronoUnit.DAYS.between(today, targetDate);
		final int incomplete = (int)linkedTaskStatuses.stream().filter(status -> status != WbsTaskStatus.DONE).count();
		return new MilestoneDeadline(dDay, dDay <= 0 && incomplete > 0, linkedTaskStatuses.size(), incomplete);
	}
}
