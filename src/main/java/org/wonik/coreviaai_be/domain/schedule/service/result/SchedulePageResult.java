package org.wonik.coreviaai_be.domain.schedule.service.result;

import java.util.List;

public record SchedulePageResult(List<ScheduleResult> items, boolean hasNext) {
	public SchedulePageResult {
		items = List.copyOf(items);
	}
}
