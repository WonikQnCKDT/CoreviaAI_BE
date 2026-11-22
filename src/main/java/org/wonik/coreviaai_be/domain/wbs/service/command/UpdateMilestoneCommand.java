package org.wonik.coreviaai_be.domain.wbs.service.command;

import java.time.LocalDate;

/**
 * 마일스톤 이름·목표일 수정. 보낸 값으로 덮어쓴다.
 *
 * @param version 조회 때 받은 값(낙관적 잠금)
 */
public record UpdateMilestoneCommand(String name, LocalDate targetDate, long version) {
}
