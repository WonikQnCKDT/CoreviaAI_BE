package org.wonik.coreviaai_be.domain.worklog.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.worklog.entity.Worklog;
import org.wonik.coreviaai_be.domain.worklog.repository.WorklogRepository;
import org.wonik.coreviaai_be.domain.worklog.service.result.WorklogResult;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.time.QueryRangePolicy;
import org.wonik.global.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorklogService {

	private final WorklogRepository repository;
	private final EntityManager entityManager;

	public List<WorklogResult> findRange(final Long userId, final LocalDate from, final LocalDate to) {
		QueryRangePolicy.requireDateRange(from, to);
		return repository.findByUserIdAndLogDateBetweenOrderByLogDateAsc(userId, from, to).stream()
			.map(WorklogResult::from).toList();
	}

	public WorklogResult get(final Long userId, final LocalDate logDate) {
		return repository.findByUserIdAndLogDate(userId, logDate)
			.map(WorklogResult::from)
			.orElseThrow(() -> new CustomException(ErrorCode.WORKLOG_NOT_FOUND));
	}

	/** 그날 업무일지가 없으면 만들고 있으면 내용을 바꾼다. 같은 날짜의 동시 첫 저장은 사용자 행 잠금으로 직렬화한다. */
	@Transactional
	public WorklogResult upsert(final Long userId, final LocalDate logDate, final String content) {
		try {
			if (entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE) == null) {
				throw new CustomException(ErrorCode.USER_NOT_FOUND);
			}
		} catch (final PessimisticLockException | LockTimeoutException ex) {
			// EntityManager 직접 호출은 Spring Data 저장소의 예외 변환을 거치지 않는다.
			throw new CustomException(ErrorCode.CONCURRENT_MODIFICATION, ex);
		}
		final Worklog log = repository.findByUserIdAndLogDate(userId, logDate)
			.map(existing -> {
				existing.replaceContent(content);
				return existing;
			})
			.orElseGet(() -> repository.save(Worklog.create(userId, logDate, content)));
		repository.flush();
		entityManager.refresh(log);
		return WorklogResult.from(log);
	}
}
