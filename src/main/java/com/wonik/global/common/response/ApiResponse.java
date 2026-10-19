package com.wonik.global.common.response;

import com.wonik.global.common.exception.ErrorCode;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
		return new ApiResponse<>(false, null, new ApiError(message, status.value()));
	}

	public static ApiResponse<?> error(final ErrorCode errorCode) {
		return new ApiResponse<>(false, null, new ApiError(errorCode.getMessage(), errorCode.getStatus()));
	}

	private record ApiError(String message, int status) {
	}
}
