package org.wonik.coreviaai_be.domain.wbs.dto.request;

import org.wonik.coreviaai_be.domain.wbs.service.WbsTaskService;
import org.wonik.coreviaai_be.domain.wbs.service.command.MoveWbsTaskCommand;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 드래그 이동. parentTaskId를 비우면 최상위 작업으로, 넣으면 그 작업의 하위작업으로 옮긴다.
 * index는 옮길 곳의 형제 목록(옮기는 항목을 뺀 목록)에서 0부터 센 위치다. 0이면 맨 앞, 형제 수와 같으면 맨 뒤.
 */
public record MoveWbsTaskRequest(
	@Positive(message = "상위 작업 ID를 확인해 주세요")
	Long parentTaskId,

	@NotNull(message = "옮길 위치(index)를 입력해 주세요")
	@PositiveOrZero(message = "옮길 위치(index)는 0 이상이어야 합니다")
	@Max(value = WbsTaskService.MAX_ITEMS_PER_PROJECT, message = "옮길 위치(index)가 너무 큽니다")
	Integer index,

	@NotNull(message = "옮길 작업의 version을 입력해 주세요")
	@PositiveOrZero(message = "version은 0 이상이어야 합니다")
	Long version
) {

	public MoveWbsTaskCommand toCommand() {
		return new MoveWbsTaskCommand(parentTaskId, index, version);
	}
}
