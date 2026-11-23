package org.wonik.coreviaai_be.domain.schedule.dto.response;

import java.util.List;

import org.wonik.coreviaai_be.domain.schedule.service.result.SchedulePageResult;

public record SchedulePageResponse(List<ScheduleResponse> items, int page, int size, boolean hasNext) {
	public static SchedulePageResponse from(final SchedulePageResult result, final int page, final int size) {
		return new SchedulePageResponse(result.items().stream().map(ScheduleResponse::from).toList(),
			page, size, result.hasNext());
	}
}
