package org.wonik.global.errorlog;

import java.time.Clock;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * 미처리 예외를 에러 로그로 저장한다(FR-ADM-09). 원래 요청의 트랜잭션과 분리된 새 트랜잭션(REQUIRES_NEW)에서 저장해,
 * 원래 작업이 롤백돼도 기록은 남고 기록이 실패해도 원래 작업에 영향이 없다.
 * 기록 실패를 응답에 반영하지 않는 처리는 호출자(GlobalExceptionHandler)가 맡는다.
 */
@Component
@RequiredArgsConstructor
public class ErrorLogRecorder {

	private final ErrorLogRepository errorLogRepository;
	private final Clock clock;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(final Throwable error, final String httpMethod, final String apiPath, final Long userId) {
		errorLogRepository.save(ErrorLog.of(clock.instant(), httpMethod, apiPath, userId, error));
	}
}
