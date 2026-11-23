package org.wonik.coreviaai_be.domain.wbs.dto.request;

import java.util.LinkedHashSet;
import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.service.TaskDependencyService;
import org.wonik.coreviaai_be.domain.wbs.service.command.ReplaceTaskPredecessorsCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 후행 작업의 선행 작업 전체 교체. 중복 ID는 한 번만 연결한다. */
public record ReplaceTaskPredecessorsRequest(
	@NotNull(message = "선행 작업 목록을 입력해 주세요. 모두 해제하려면 빈 배열을 보내 주세요")
	@Size(max = TaskDependencyService.MAX_PREDECESSORS_PER_TASK, message = "선행 작업은 최대 200개까지 지정할 수 있습니다")
	List<@NotNull(message = "선행 작업 ID를 확인해 주세요") @Positive(message = "선행 작업 ID를 확인해 주세요") Long> predecessorTaskIds
) {

	public ReplaceTaskPredecessorsCommand toCommand() {
		return new ReplaceTaskPredecessorsCommand(new LinkedHashSet<>(predecessorTaskIds));
	}
}
