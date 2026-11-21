package org.wonik.coreviaai_be.domain.wbs.service.command;

import java.time.LocalDate;
import java.util.List;

import org.wonik.coreviaai_be.domain.project.entity.type.WaterfallPhaseType;

/**
 * AI 초안 생성에 넘길 프로젝트 정보. PM 권한을 확인한 뒤 읽은 값만 담는다(aiserver는 권한을 다시 검사하지 않음).
 *
 * @param startDate     프로젝트 시작일. 선택 입력이라 null일 수 있다(FR-PRJ-01)
 * @param endDate       프로젝트 종료일. 선택 입력이라 null일 수 있다
 * @param phases        프로젝트 단계(순서대로). AI가 단계별로 작업을 나누는 기준이다
 * @param existingTasks 이미 등록된 작업(중복 제안 방지용)
 * @param members       담당자 제안에 쓰는 멤버
 */
public record WbsDraftContext(
	Long projectId,
	String projectName,
	LocalDate startDate,
	LocalDate endDate,
	List<WaterfallPhaseType> phases,
	List<DraftExistingTask> existingTasks,
	List<DraftTeamMember> members,
	GenerateWbsDraftCommand command
) {
}
