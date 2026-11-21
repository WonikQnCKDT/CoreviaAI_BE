package org.wonik.coreviaai_be.domain.wbs.dto.request;

import java.time.LocalDate;

import org.wonik.coreviaai_be.domain.wbs.entity.Milestone;
import org.wonik.coreviaai_be.domain.wbs.service.command.UpdateMilestoneCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 마일스톤 이름·목표일 수정. 보낸 값으로 덮어쓴다. version은 조회 응답 값이며 그 사이 다른 변경이 있었으면 409.
 */
public record UpdateMilestoneRequest(
	@NotBlank(message = "마일스톤명을 입력해 주세요")
	@Size(max = Milestone.NAME_MAX_LENGTH, message = "마일스톤명은 200자 이하로 입력해 주세요")
	String name,

	@NotNull(message = "마일스톤 목표일을 입력해 주세요")
	LocalDate targetDate,

	@NotNull(message = "수정할 마일스톤의 version을 입력해 주세요")
	@PositiveOrZero(message = "version은 0 이상이어야 합니다")
	Long version
) {

	public UpdateMilestoneCommand toCommand() {
		return new UpdateMilestoneCommand(name.strip(), targetDate, version);
	}
}
