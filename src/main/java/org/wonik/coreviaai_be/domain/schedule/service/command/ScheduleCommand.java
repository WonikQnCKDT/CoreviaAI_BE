package org.wonik.coreviaai_be.domain.schedule.service.command;

import java.time.Instant;

/** 일정 등록·수정 공통 입력. 수정은 보낸 값으로 덮어쓴다. */
public record ScheduleCommand(String title, Instant startAt, Instant endAt, boolean allDay, String memo) {
}
