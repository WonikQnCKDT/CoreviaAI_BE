package org.wonik.coreviaai_be.domain.wbs.service.result;

import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;

/**
 * WBS 트리의 항목 하나와 그 하위작업(표시 순서대로).
 *
 * @param assigneeName 담당자 이름. 담당자가 없으면 null
 */
public record WbsTreeNode(WbsTask task, String assigneeName, List<WbsTreeNode> subtasks) {

	/** 하위작업이 있어 기간이 하위작업 기간으로 산출된 작업인지. */
	public boolean scheduleDerived() {
		return !subtasks.isEmpty();
	}
}
