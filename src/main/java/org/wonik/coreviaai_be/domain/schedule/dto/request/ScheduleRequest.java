package org.wonik.coreviaai_be.domain.schedule.dto.request;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.schedule.entity.PersonalEvent;
import org.wonik.coreviaai_be.domain.schedule.service.command.ScheduleCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 일정 등록·수정 요청. 수정도 같은 형식이며 보낸 값으로 덮어쓴다. */
public record ScheduleRequest(
	@NotBlank @Size(max = PersonalEvent.TITLE_MAX_LENGTH) @Pattern(regexp = "[^\\p{Cntrl}]*") String title,
	@NotNull Instant startAt,
	@NotNull Instant endAt,
	/** 생략하면 종일이 아니다. 래퍼 타입으로 받아야 생략(null)을 허용한다. */
	Boolean allDay,
	@Size(max = PersonalEvent.MEMO_MAX_LENGTH) String memo) {

	public ScheduleCommand toCommand() {
		return new ScheduleCommand(title.strip(), startAt, endAt, Boolean.TRUE.equals(allDay), memo);
	}

	@Override
	public String toString() {
		return "ScheduleRequest[redacted]";
	}
}
