package org.wonik.coreviaai_be.domain.wbs.service.command;

import java.util.Set;

/**
 * 마일스톤 연결 작업을 보낸 목록으로 통째로 바꾼다(빈 집합이면 모두 해제).
 *
 * @param taskIds 연결할 작업(중복 제거된 집합)
 * @param version 조회 때 받은 마일스톤 version(낙관적 잠금)
 */
public record ReplaceMilestoneTasksCommand(Set<Long> taskIds, long version) {
}
