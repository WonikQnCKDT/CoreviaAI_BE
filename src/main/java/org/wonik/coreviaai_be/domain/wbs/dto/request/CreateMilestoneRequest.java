package org.wonik.coreviaai_be.domain.wbs.dto.request;

import java.time.LocalDate;
import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.entity.Milestone;
import org.wonik.coreviaai_be.domain.wbs.service.MilestoneService;
import org.wonik.coreviaai_be.domain.wbs.service.command.CreateMilestoneCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 마일스톤 등록. 연결 작업은 선택이며 같은 프로젝트의 WBS 작업·하위작업 ID만 쓸 수 있다.
 */
public record CreateMilestoneRequest(
	@NotBlank(message = "마일스톤명을 입력해 주세요")
	@Size(max = Milestone.NAME_MAX_LENGTH, message = "마일스톤명은 200자 이하로 입력해 주세요")
	String name,

	@NotNull(message = "마일스톤 목표일을 입력해 주세요")
	LocalDate targetDate,

	@Size(max = MilestoneService.MAX_TASKS_PER_MILESTONE, message = "연결 작업은 최대 200개까지 지정할 수 있습니다")
	List<@NotNull(message = "연결 작업 ID를 확인해 주세요") @Positive(message = "연결 작업 ID를 확인해 주세요") Long> taskIds
) {

	public CreateMilestoneCommand toCommand() {
		return new CreateMilestoneCommand(name.strip(), targetDate, MilestoneTaskIds.distinct(taskIds));
	}
}
