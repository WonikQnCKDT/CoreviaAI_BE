package org.wonik.coreviaai_be.domain.wbs.service.command;

import java.time.LocalDate;
import java.util.Set;

/**
 * 마일스톤 직접 등록(출처 MANUAL).
 *
 * @param taskIds 처음부터 연결할 작업(중복 제거된 순서 있는 집합, 없으면 빈 집합)
 */
public record CreateMilestoneCommand(String name, LocalDate targetDate, Set<Long> taskIds) {
}
