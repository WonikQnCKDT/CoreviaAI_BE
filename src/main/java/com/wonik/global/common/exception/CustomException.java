package com.wonik.global.common.exception;

public class CustomException extends RuntimeException {
	private final ErrorCode errorCode;

	public CustomException(final ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	ErrorCode getErrorCode() {
		return errorCode;
	}
}
