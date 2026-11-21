package org.wonik.coreviaai_be.domain.wbs.service.command;

import org.wonik.coreviaai_be.domain.wbs.entity.TaskDetails;

/**
 * 작업·하위작업 기본 필드 수정. 보낸 값으로 덮어쓴다.
 *
 * @param version 조회 때 받은 값(낙관적 잠금)
 */
public record UpdateWbsTaskCommand(TaskDetails details, long version) {
}
