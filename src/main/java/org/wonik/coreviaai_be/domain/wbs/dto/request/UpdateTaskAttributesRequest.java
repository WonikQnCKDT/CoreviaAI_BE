package org.wonik.coreviaai_be.domain.wbs.dto.request;

import org.wonik.coreviaai_be.domain.wbs.entity.TaskAttributes;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskPriority;
import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskType;
import org.wonik.coreviaai_be.domain.wbs.service.command.UpdateTaskAttributesCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 업무 상세 필드 수정(FR-TSK-01). 세 필드를 모두 보낸 값으로 덮어쓴다(비운 완료 조건은 지움).
 * version은 업무 상세·WBS 트리 응답 값이며, 그 사이 다른 변경이 있었으면 409로 거부한다.
 */
public record UpdateTaskAttributesRequest(
	@NotNull(message = "업무 유형을 선택해 주세요")
	TaskType taskType,

	@NotNull(message = "우선순위를 선택해 주세요")
	TaskPriority priority,

	@Size(max = WbsTask.COMPLETION_CRITERIA_MAX_LENGTH, message = "완료 조건은 2000자 이하로 입력해 주세요")
	String completionCriteria,

	@NotNull(message = "수정할 업무의 version을 입력해 주세요")
	@PositiveOrZero(message = "version은 0 이상이어야 합니다")
	Long version
) {

	public UpdateTaskAttributesCommand toCommand() {
		return new UpdateTaskAttributesCommand(
			new TaskAttributes(taskType, priority, DraftText.trimToNull(completionCriteria)), version);
	}
}
