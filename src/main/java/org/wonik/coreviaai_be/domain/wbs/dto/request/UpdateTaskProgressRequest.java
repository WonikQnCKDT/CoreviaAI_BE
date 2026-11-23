package org.wonik.coreviaai_be.domain.wbs.dto.request;

import org.wonik.coreviaai_be.domain.wbs.service.command.UpdateTaskProgressCommand;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** 업무 진척률 수정(SCRUM-119). */
public record UpdateTaskProgressRequest(
	@NotNull(message = "진척률을 입력해 주세요")
	@Min(value = 0, message = "진척률은 0 이상이어야 합니다")
	@Max(value = 100, message = "진척률은 100 이하여야 합니다")
	Integer progress,

	@NotNull(message = "수정할 작업의 version을 입력해 주세요")
	@PositiveOrZero(message = "version은 0 이상이어야 합니다")
	Long version
) {
	public UpdateTaskProgressCommand toCommand() {
		return new UpdateTaskProgressCommand(progress, version);
	}
}
