package org.wonik.global.common.response;

import org.wonik.global.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
@RequiredArgsConstructor
public class ApiResponse<T> {
	private final boolean success;
	private final T response;
	private final ApiError error;

	public static <T> ApiResponse<T> success(final T response) {
		return new ApiResponse<>(true, response, null);
	}

	public static ApiResponse<?> error(final String message, final HttpStatusCode status) {
		return new ApiResponse<>(false, null, new ApiError(statusCodeName(status), message, status.value()));
	}

	public static ApiResponse<?> error(final ErrorCode errorCode) {
		return new ApiResponse<>(false, null, new ApiError(errorCode.name(), errorCode.getMessage(), errorCode.getStatus()));
	}

	public static <T> ApiResponse<T> error(final T response, final ErrorCode errorCode) {
		return new ApiResponse<>(false, response, new ApiError(errorCode.name(), errorCode.getMessage(), errorCode.getStatus()));
	}

	private static String statusCodeName(final HttpStatusCode status) {
		final HttpStatus resolved = HttpStatus.resolve(status.value());
		return resolved != null ? resolved.name() : "HTTP_" + status.value();
	}

	public record ApiError(String code, String message, int status) {
	}
}
