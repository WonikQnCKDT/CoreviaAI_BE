package org.wonik.coreviaai_be.domain.wbs.dto.request;

import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.service.MilestoneService;
import org.wonik.coreviaai_be.domain.wbs.service.command.ReplaceMilestoneTasksCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 마일스톤 연결 작업 전체 교체. taskIds가 새 연결 전체이며 빈 배열이면 모두 해제한다.
 */
public record ReplaceMilestoneTasksRequest(
	@NotNull(message = "연결 작업 목록을 입력해 주세요. 모두 해제하려면 빈 배열을 보내 주세요")
	@Size(max = MilestoneService.MAX_TASKS_PER_MILESTONE, message = "연결 작업은 최대 200개까지 지정할 수 있습니다")
	List<@NotNull(message = "연결 작업 ID를 확인해 주세요") @Positive(message = "연결 작업 ID를 확인해 주세요") Long> taskIds,

	@NotNull(message = "수정할 마일스톤의 version을 입력해 주세요")
	@PositiveOrZero(message = "version은 0 이상이어야 합니다")
	Long version
) {

	public ReplaceMilestoneTasksCommand toCommand() {
		return new ReplaceMilestoneTasksCommand(MilestoneTaskIds.distinct(taskIds), version);
	}
}
