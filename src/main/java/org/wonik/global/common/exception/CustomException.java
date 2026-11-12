package org.wonik.global.common.exception;

public class CustomException extends RuntimeException {
	private final ErrorCode errorCode;

	public CustomException(final ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	/** 하위 예외를 도메인 오류로 바꿀 때 원인을 보존한다(운영 진단용, 응답에는 노출하지 않음). */
	public CustomException(final ErrorCode errorCode, final Throwable cause) {
		super(errorCode.getMessage(), cause);
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
