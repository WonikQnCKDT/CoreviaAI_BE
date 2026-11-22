package org.wonik.coreviaai_be.domain.wbs.service.result;

import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;

/**
 * 업무 상세 조회 결과.
 *
 * @param assigneeName    담당자 이름. 담당자가 없거나 찾지 못하면 null
 * @param createdByName   등록자 이름. 찾지 못하면 null
 * @param scheduleDerived 하위작업이 있어 기간이 하위작업 기간으로 산출된 작업인지
 */
public record TaskDetail(WbsTask task, String assigneeName, String createdByName, boolean scheduleDerived) {
}
