package org.wonik.coreviaai_be.domain.wbs.service.command;

import org.wonik.coreviaai_be.domain.wbs.entity.TaskAttributes;

/**
 * 업무 상세 필드(유형·우선순위·완료 조건) 수정(FR-TSK-01). 보낸 값으로 덮어쓴다.
 *
 * @param version 조회 때 받은 값(낙관적 잠금, WBS 기본 필드 수정과 같은 version)
 */
public record UpdateTaskAttributesCommand(TaskAttributes attributes, long version) {
}
