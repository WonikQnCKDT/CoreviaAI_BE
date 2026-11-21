package org.wonik.coreviaai_be.domain.wbs.service.result;

import java.util.List;

/**
 * AI가 제안한 WBS 초안(FR-PRJ-07). 저장하지 않고 PM 검토 화면에 그대로 보낸다(NFR-08: 승인 전 저장 없음).
 * 제안값(기간·공수·담당자)은 참고값이며, PM이 승인·수정한 값만 반영 API로 다시 받는다.
 */
public record WbsDraftProposal(
	List<ProposedStage> stages,
	List<ProposedMilestone> milestones,
	DraftFeasibility feasibility,
	List<String> warnings,
	List<String> suggestions,
	List<String> assumptions,
	List<String> clarificationQuestions
) {

	public int taskCount() {
		return stages.stream().mapToInt(stage -> stage.tasks().size()).sum();
	}

	public int subtaskCount() {
		return stages.stream()
			.flatMap(stage -> stage.tasks().stream())
			.mapToInt(task -> task.subtasks().size())
			.sum();
	}
}
