package org.wonik.global.common.exception;

public class CustomException extends RuntimeException {
	private final ErrorCode errorCode;
	private final Object response;

	public CustomException(final ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
		this.response = null;
	}

	/** 하위 예외를 도메인 오류로 바꿀 때 원인을 보존한다(운영 진단용, 응답에는 노출하지 않음). */
	public CustomException(final ErrorCode errorCode, final Throwable cause) {
		super(errorCode.getMessage(), cause);
		this.errorCode = errorCode;
		this.response = null;
	}

	/** 오류와 함께 클라이언트가 후속 조치에 사용할 안전한 업무 데이터를 돌려준다. */
	public CustomException(final ErrorCode errorCode, final Object response) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
		this.response = response;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public Object getResponse() {
		return response;
	}
}
