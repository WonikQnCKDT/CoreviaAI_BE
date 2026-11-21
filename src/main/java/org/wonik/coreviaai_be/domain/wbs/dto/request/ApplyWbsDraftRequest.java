package org.wonik.coreviaai_be.domain.wbs.dto.request;

import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.service.command.ApplyWbsDraftCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * AI 초안 반영 요청(FR-PRJ-07). PM이 승인한 항목만 수정값과 함께 보내고, 제외한 항목은 개수만 보낸다.
 * 항목 간 참조(선후행·마일스톤 연결)는 초안의 tempId로 표현한다.
 */
public record ApplyWbsDraftRequest(
	@NotBlank(message = "초안 토큰이 필요합니다")
	@Size(max = 64, message = "초안 토큰 형식이 올바르지 않습니다")
	String draftToken,

	@NotNull(message = "반영할 작업 목록이 필요합니다")
	@Size(max = 200, message = "한 번에 반영할 수 있는 작업은 최대 200개입니다")
	List<@NotNull(message = "작업 값을 확인해 주세요") @Valid ApplyDraftTaskRequest> tasks,

	@NotNull(message = "반영할 마일스톤 목록이 필요합니다")
	@Size(max = 50, message = "한 번에 반영할 수 있는 마일스톤은 최대 50개입니다")
	List<@NotNull(message = "마일스톤 값을 확인해 주세요") @Valid ApplyDraftMilestoneRequest> milestones,

	@NotNull(message = "제외한 항목 수가 필요합니다")
	@PositiveOrZero(message = "제외한 항목 수를 확인해 주세요")
	@Max(value = 1000, message = "제외한 항목 수를 확인해 주세요")
	Integer rejectedCount
) {

	@AssertTrue(message = "반영할 항목을 하나 이상 선택해 주세요")
	public boolean isAnyItemSelected() {
		return (tasks != null && !tasks.isEmpty()) || (milestones != null && !milestones.isEmpty());
	}

	public ApplyWbsDraftCommand toCommand() {
		return new ApplyWbsDraftCommand(
			draftToken,
			tasks.stream().map(ApplyDraftTaskRequest::toCommand).toList(),
			milestones.stream().map(ApplyDraftMilestoneRequest::toCommand).toList(),
			rejectedCount
		);
	}
}
