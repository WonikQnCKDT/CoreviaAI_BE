package org.wonik.global.common.exception;

import org.wonik.global.common.response.ApiResponse;
import org.wonik.global.errorlog.ErrorLogRecorder;
import org.wonik.global.jwt.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * 전역 API 오류 응답. 의도한 오류(업무 예외·입력 오류·인증 실패)는 오류 코드별 응답을, 그 밖의 미처리 예외는
 * 공통 500 응답(메시지·스택 트레이스 미노출)을 주고 에러 로그(FR-ADM-09)에 남긴다.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private final ErrorLogRecorder errorLogRecorder;

	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<ApiResponse<?>> handleUsernameNotFoundException(final UsernameNotFoundException ex) {
		log.warn("인증 사용자 조회에 실패했습니다");
		return ResponseEntity
			.status(ErrorCode.INVALID_CREDENTIALS.getStatus())
			.body(ApiResponse.error(ErrorCode.INVALID_CREDENTIALS));
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiResponse<?>> handleBadCredentialsException(final BadCredentialsException ex) {
		log.warn("자격 증명 검증에 실패했습니다");
		return ResponseEntity
			.status(ErrorCode.INVALID_CREDENTIALS.getStatus())
			.body(ApiResponse.error(ErrorCode.INVALID_CREDENTIALS));
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseEntity<ApiResponse<?>> handleNoHandlerFoundException(final NoHandlerFoundException ex) {
		log.error("NoHandlerFoundException : {}", ex.getMessage());
		return ResponseEntity
			.status(ErrorCode.NOT_FOUND.getStatus())
			.body(ApiResponse.error(ErrorCode.NOT_FOUND));
	}

	/** 매핑이 없는 경로(정적 리소스 처리기까지 내려간 요청)도 다른 404와 같은 본문으로 응답한다. 미처리 예외가 아니라 기록하지 않는다. */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<?>> handleNoResourceFoundException(final NoResourceFoundException ex) {
		log.debug("매핑되지 않은 경로 요청");
		return ResponseEntity
			.status(ErrorCode.NOT_FOUND.getStatus())
			.body(ApiResponse.error(ErrorCode.NOT_FOUND));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiResponse<?>> handleHttpRequestMethodNotSupportedException(
		final HttpRequestMethodNotSupportedException ex
	) {
		log.error("HttpRequestMethodNotSupportedException : {}", ex.getMessage());
		return ResponseEntity
			.status(ErrorCode.NOT_FOUND.getStatus())
			.body(ApiResponse.error(ErrorCode.NOT_FOUND));
	}

	@ExceptionHandler(AuthorizationDeniedException.class)
	public ResponseEntity<ApiResponse<?>> handleAuthorizationDeniedException(final AuthorizationDeniedException ex) {
		final ErrorCode errorCode;
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (null == authentication || authentication instanceof AnonymousAuthenticationToken) {
			errorCode = ErrorCode.UNAUTHORIZED;
		} else {
			errorCode = ErrorCode.NOT_FOUND;
		}
		log.error("Access Denied : {}", errorCode.getMessage());

		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResponse.error(errorCode));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<?>> handleValidationException(final MethodArgumentNotValidException ex) {
		log.debug("요청 본문 검증에 실패했습니다");
		final BindingResult bindingResult = ex.getBindingResult();
		final List<FieldError> fieldErrors = bindingResult.getFieldErrors();

		final String errorMessage = fieldErrors.stream()
			.findFirst()
			.map(FieldError::getDefaultMessage)
			.orElse("Validation error");

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error(errorMessage, HttpStatus.BAD_REQUEST));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<?>> handleUnreadableJson(final HttpMessageNotReadableException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error("요청 JSON 형식을 확인해 주세요", HttpStatus.BAD_REQUEST));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiResponse<?>> handleConstraintViolationException(final ConstraintViolationException ex) {
		log.debug("요청 파라미터 검증에 실패했습니다");
		final String errorMessage = ex.getConstraintViolations().stream()
			.findFirst()
			.map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
			.orElse("Validation error");

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error(errorMessage, HttpStatus.BAD_REQUEST));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<?>> handleMethodArgumentTypeMismatchException(
		final MethodArgumentTypeMismatchException ex
	) {
		log.error("MethodArgumentTypeMismatchException : {}", ex.getMessage());
		final String name = ex.getName();
		final String message = String.format("%s의 형식이 잘못되었습니다", name);

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error(message, HttpStatus.BAD_REQUEST));
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ApiResponse<?>> handleMethodValidationException(
		final HandlerMethodValidationException ex
	) {
		log.debug("요청 파라미터 범위 검증에 실패했습니다");
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error("요청 파라미터 범위를 확인해주세요", HttpStatus.BAD_REQUEST));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<?>> handleMissingParams(final MissingServletRequestParameterException ex) {
		log.error("MissingServletRequestParameterException : {}", ex.getMessage());
		final String name = ex.getParameterName();
		final String message = String.format("필수 요청 파라미터 '%s'가 누락되었습니다", name);

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(ApiResponse.error(message, HttpStatus.BAD_REQUEST));
	}

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ApiResponse<?>> handleCustomException(final CustomException ex) {
		log.warn("처리 가능한 애플리케이션 오류: {}", ex.getErrorCode().name());
		final ErrorCode errorCode = ex.getErrorCode();
		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResponse.error(errorCode));
	}

	/**
	 * 최종 처리 지점: 위 처리기에 해당하지 않는 예외. 응답은 공통 500으로 고정하고 에러 로그에 남긴다.
	 * <p>
	 * 스프링 웹 표준 예외 중 4xx(지원하지 않는 Content-Type, 필수 헤더 누락 등)와 끊긴 연결은 미처리 예외가 아니므로
	 * 다시 던져 스프링 기본 처리기(DefaultHandlerExceptionResolver)에 맡긴다(이 처리기를 넓히기 전과 같은 응답).
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<?>> handleUnexpectedException(
		final Exception ex,
		final HttpServletRequest request
	) throws Exception {
		if (isClientOrConnectionError(ex)) {
			throw ex;
		}
		log.error("처리되지 않은 서버 오류", ex);
		recordErrorLog(ex, request);
		return ResponseEntity
			.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
			.body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
	}

	private static boolean isClientOrConnectionError(final Exception ex) {
		return ex instanceof AsyncRequestNotUsableException
			|| (ex instanceof final ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError());
	}

	/** 기록 실패가 원래 오류 응답을 바꾸거나 새 예외가 되지 않게 한다. 실패는 애플리케이션 로그로만 남긴다. */
	private void recordErrorLog(final Exception ex, final HttpServletRequest request) {
		try {
			errorLogRecorder.record(ex, request.getMethod(), request.getRequestURI(), currentUserId());
		} catch (final RuntimeException recordFailure) {
			log.error("에러 로그를 저장하지 못했습니다", recordFailure);
		}
	}

	private static Long currentUserId() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof final LoginUser loginUser) {
			return loginUser.userId();
		}
		return null;
	}
}
