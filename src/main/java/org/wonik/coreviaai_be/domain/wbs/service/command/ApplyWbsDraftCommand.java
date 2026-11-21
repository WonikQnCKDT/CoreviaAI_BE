package org.wonik.coreviaai_be.domain.wbs.service.command;

import java.util.List;

/**
 * PM이 승인한 AI 초안 항목(FR-PRJ-07). 제외한 항목은 보내지 않고 개수만 받는다(NFR-11 승인률 수집).
 *
 * @param draftToken    초안 생성 응답의 토큰. 한 번만 반영할 수 있다
 * @param tasks         승인한 작업(하위작업 포함)
 * @param milestones    승인한 마일스톤
 * @param rejectedCount 제외한 항목 수
 */
public record ApplyWbsDraftCommand(
	String draftToken,
	List<DraftTaskCommand> tasks,
	List<DraftMilestoneCommand> milestones,
	int rejectedCount
) {
}
