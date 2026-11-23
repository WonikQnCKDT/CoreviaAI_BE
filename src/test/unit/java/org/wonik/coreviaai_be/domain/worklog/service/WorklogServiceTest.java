package org.wonik.coreviaai_be.domain.worklog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.worklog.repository.WorklogRepository;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.user.entity.User;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;

class WorklogServiceTest {

	@Test
	void failedOwnerLockReturnsConflictWithoutWritingOrExposingDatabaseDetails() {
		for (final RuntimeException failure : List.of(new LockTimeoutException("private lock details"),
			new PessimisticLockException("private lock details"))) {
			final WorklogRepository repository = mock(WorklogRepository.class);
			final EntityManager entityManager = mock(EntityManager.class);
			when(entityManager.find(User.class, 7L, LockModeType.PESSIMISTIC_WRITE)).thenThrow(failure);
			final WorklogService service = new WorklogService(repository, entityManager);
			final CustomException error = catchThrowableOfType(CustomException.class,
				() -> service.upsert(7L, LocalDate.of(2026, 11, 23), "private content"));
			assertThat(error.getErrorCode()).isEqualTo(ErrorCode.CONCURRENT_MODIFICATION);
			assertThat(error.getErrorCode().getStatus()).isEqualTo(409);
			assertThat(error.getCause()).isSameAs(failure);
			assertThat(error.getResponse()).isNull();
			assertThat(error.getMessage()).doesNotContain("private lock details", "private content");
			verifyNoInteractions(repository);
		}
	}

	@Test
	void missingOwnerDoesNotCreateAnOrphanWorklog() {
		final WorklogRepository repository = mock(WorklogRepository.class);
		final WorklogService service = new WorklogService(repository, mock(EntityManager.class));
		final CustomException error = catchThrowableOfType(CustomException.class,
			() -> service.upsert(7L, LocalDate.of(2026, 11, 23), "content"));
		assertThat(error.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
		verifyNoInteractions(repository);
	}
}
