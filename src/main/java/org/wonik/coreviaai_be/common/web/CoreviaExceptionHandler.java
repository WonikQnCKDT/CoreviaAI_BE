package org.wonik.coreviaai_be.common.web;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * 업무 모듈(coreviaai_be) API의 동시 수정 충돌(낙관적 잠금)을 409로 응답한다.
 * 예: 두 관리자가 같은 사용자를 동시에 바꾸거나, PM 위임과 관리자 재지정이 같은 프로젝트에서 겹친 경우 나중 요청.
 * <p>
 * 전역 처리기(GlobalExceptionHandler)를 바꾸면 authserver(로그인 API)의 응답까지 달라지므로 업무 모듈 패키지로 범위를
 * 한정하고, RuntimeException을 처리하는 전역 처리기보다 먼저 적용되도록 우선순위를 높인다.
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "org.wonik.coreviaai_be")
public class CoreviaExceptionHandler {

	@ExceptionHandler(OptimisticLockingFailureException.class)
	public ResponseEntity<ApiResponse<?>> handleOptimisticLockingFailure(final OptimisticLockingFailureException ex) {
		log.warn("업무 API 동시 수정 충돌", ex);
		return ResponseEntity
			.status(ErrorCode.CONCURRENT_MODIFICATION.getStatus())
			.body(ApiResponse.error(ErrorCode.CONCURRENT_MODIFICATION));
	}
}
