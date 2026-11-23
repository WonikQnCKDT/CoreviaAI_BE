package org.wonik.coreviaai_be.domain.wbs.service.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 승인한 작업 한 개(PM 수정 반영). phaseId는 같은 프로젝트의 사용자 정의 단계다.
 * 요청 순서가 곧 표시 순서다.
 *
 * @param tempId       초안의 임시 ID. 선후행·마일스톤 연결과 응답의 ID 매핑에 쓴다
 * @param dependencies 선행 작업의 tempId(같은 요청 안의 작업만)
 */
public record DraftTaskCommand(
	Long phaseId,
	String tempId,
	String name,
	String description,
	LocalDate startDate,
	LocalDate endDate,
	BigDecimal estimatedPersonDays,
	Long assigneeUserId,
	List<String> dependencies,
	List<DraftSubtaskCommand> subtasks
) {
	public DraftTaskCommand(final String tempId, final String name, final String description,
		final LocalDate startDate, final LocalDate endDate, final BigDecimal estimatedPersonDays,
		final Long assigneeUserId, final List<String> dependencies, final List<DraftSubtaskCommand> subtasks) {
		this(null, tempId, name, description, startDate, endDate, estimatedPersonDays, assigneeUserId,
			dependencies, subtasks);
	}
}
