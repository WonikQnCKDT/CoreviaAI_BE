package org.wonik.coreviaai_be.domain.wbs.service.result;

import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;

/**
 * 보드 카드 하나.
 *
 * @param parentTaskName 하위작업이면 상위 작업 이름, 작업이면 null
 * @param assigneeName   담당자 이름. 담당자가 없거나 찾지 못하면 null
 */
public record BoardCard(WbsTask task, String parentTaskName, String assigneeName) {
}
