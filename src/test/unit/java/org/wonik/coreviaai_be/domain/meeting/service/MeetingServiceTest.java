package org.wonik.coreviaai_be.domain.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.meeting.policy.MeetingAccessPolicy;
import org.wonik.coreviaai_be.domain.meeting.repository.MeetingCreateRequestRepository;
import org.wonik.coreviaai_be.domain.meeting.repository.MeetingRepository;
import org.wonik.coreviaai_be.domain.meeting.service.command.SaveMeetingCommand;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;

class MeetingServiceTest {
	@Test
	void lockTimeoutReturnsConflictWithoutCreatingARequest() {
		final MeetingRepository meetings = mock(MeetingRepository.class);
		final MeetingCreateRequestRepository requests = mock(MeetingCreateRequestRepository.class);
		final EntityManager entityManager = mock(EntityManager.class);
		when(entityManager.find(User.class, 7L, LockModeType.PESSIMISTIC_WRITE))
			.thenThrow(new LockTimeoutException("private database detail"));
		final MeetingService service = new MeetingService(meetings, requests, mock(MeetingAccessPolicy.class),
			mock(UserRepository.class), mock(AuditLogger.class), entityManager);
		final SaveMeetingCommand command = new SaveMeetingCommand("회의", Instant.parse("2026-11-24T00:00:00Z"),
			null, List.of(), List.of(), List.of());
		final CustomException failure = catchThrowableOfType(CustomException.class,
			() -> service.create(7L, 3L, UUID.randomUUID(), command));
		assertThat(failure.getErrorCode()).isEqualTo(ErrorCode.CONCURRENT_MODIFICATION);
		assertThat(failure.getMessage()).doesNotContain("private database detail");
		verifyNoInteractions(meetings, requests);
	}
}
