package org.wonik.coreviaai_be.domain.wbs.dto.response;

import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.service.result.MilestoneView;

/**
 * 프로젝트 마일스톤 전체(목표일 순, 같으면 ID 순). 프로젝트당 MilestoneService.MAX_MILESTONES_PER_PROJECT(200)개로
 * 제한해 페이지로 나누지 않는다(간트·대시보드가 전체를 한 번에 그린다).
 */
public record MilestoneListResponse(List<MilestoneResponse> milestones) {

	public static MilestoneListResponse from(final List<MilestoneView> views) {
		return new MilestoneListResponse(MilestoneResponse.listOf(views));
	}
}
