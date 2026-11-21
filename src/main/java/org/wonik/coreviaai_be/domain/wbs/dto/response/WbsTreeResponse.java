package org.wonik.coreviaai_be.domain.wbs.dto.response;

import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.service.result.WbsTreeNode;

/**
 * 프로젝트 WBS 전체(작업 표시 순서대로, 각 작업 아래 하위작업). 페이지로 나누지 않는다: 드래그 편집에 트리 전체가 필요하고
 * 프로젝트당 항목 수를 WbsTaskService.MAX_ITEMS_PER_PROJECT(2,000)로 제한한다.
 */
public record WbsTreeResponse(List<WbsTaskResponse> tasks) {

	public static WbsTreeResponse from(final List<WbsTreeNode> tree) {
		return new WbsTreeResponse(WbsTaskResponse.listOf(tree));
	}
}
