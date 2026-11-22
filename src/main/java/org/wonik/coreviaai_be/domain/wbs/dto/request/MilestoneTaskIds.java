package org.wonik.coreviaai_be.domain.wbs.dto.request;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 요청의 연결 작업 ID 목록을 중복 없는 집합으로 바꾼다. 같은 ID를 여러 번 보내도 한 번만 연결한다(UNIQUE(milestone_id, task_id)).
 */
final class MilestoneTaskIds {

	private MilestoneTaskIds() {
	}

	static Set<Long> distinct(final List<Long> taskIds) {
		return taskIds == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(taskIds));
	}
}
