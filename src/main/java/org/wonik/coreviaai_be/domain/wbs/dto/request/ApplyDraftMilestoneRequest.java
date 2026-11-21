package org.wonik.coreviaai_be.domain.wbs.dto.request;

import java.time.LocalDate;
import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.entity.Milestone;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftMilestoneCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 반영할 마일스톤 한 개. 연결 작업은 같은 요청에서 반영하는 작업의 tempId만 쓸 수 있다.
 */
public record ApplyDraftMilestoneRequest(
	@NotBlank(message = "마일스톤의 임시 ID가 필요합니다")
	@Size(max = 50, message = "임시 ID는 50자 이하여야 합니다")
	String tempId,

	@NotBlank(message = "마일스톤명을 입력해 주세요")
	@Size(max = Milestone.NAME_MAX_LENGTH, message = "마일스톤명은 200자 이하로 입력해 주세요")
	String name,

	@NotNull(message = "마일스톤 목표일을 입력해 주세요")
	LocalDate targetDate,

	@Size(max = 200, message = "연결 작업은 최대 200개까지 지정할 수 있습니다")
	List<@NotBlank(message = "연결 작업의 임시 ID를 확인해 주세요") String> relatedTaskIds
) {

	public DraftMilestoneCommand toCommand() {
		return new DraftMilestoneCommand(tempId, name.strip(), targetDate,
			relatedTaskIds == null ? List.of() : relatedTaskIds);
	}
}
