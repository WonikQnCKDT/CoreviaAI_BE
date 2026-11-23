package org.wonik.coreviaai_be.domain.schedule.service;

import java.time.Instant;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.schedule.entity.PersonalEvent;
import org.wonik.coreviaai_be.domain.schedule.policy.SchedulePolicy;
import org.wonik.coreviaai_be.domain.schedule.repository.PersonalEventRepository;
import org.wonik.coreviaai_be.domain.schedule.service.command.ScheduleCommand;
import org.wonik.coreviaai_be.domain.schedule.service.result.ScheduleResult;
import org.wonik.coreviaai_be.domain.schedule.service.result.SchedulePageResult;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.time.QueryRangePolicy;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalScheduleService {

	private final PersonalEventRepository repository;
	private final EntityManager entityManager;

	public SchedulePageResult findOverlapping(final Long userId, final Instant from, final Instant to,
		final int page, final int size) {
		QueryRangePolicy.requireInstantRange(from, to);
		final var events = repository.findOverlapping(userId, from, to, PageRequest.of(page, size));
		return new SchedulePageResult(events.stream().map(ScheduleResult::from).toList(), events.hasNext());
	}

	@Transactional
	public ScheduleResult create(final Long userId, final ScheduleCommand command) {
		SchedulePolicy.validatePeriod(command.startAt(), command.endAt());
		final PersonalEvent event = repository.saveAndFlush(PersonalEvent.create(userId, command.title(),
			command.startAt(), command.endAt(), command.allDay(), command.memo()));
		// 응답 시각을 DB 정밀도(TIMESTAMP(6))에 맞춘다.
		entityManager.refresh(event);
		return ScheduleResult.from(event);
	}

	@Transactional
	public ScheduleResult update(final Long userId, final Long eventId, final ScheduleCommand command) {
		SchedulePolicy.validatePeriod(command.startAt(), command.endAt());
		final PersonalEvent event = repository.findByIdAndUserId(eventId, userId)
			.orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
		event.update(command.title(), command.startAt(), command.endAt(), command.allDay(), command.memo());
		repository.flush();
		entityManager.refresh(event);
		return ScheduleResult.from(event);
	}
}
