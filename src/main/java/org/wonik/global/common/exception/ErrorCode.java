package org.wonik.global.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
	INVALID_CREDENTIALS(400, "아이디나 패스워드가 일치하지 않습니다."),
	INVALID_PASSWORD(400, "잘못된 비밀번호입니다"),
	INVALID_NEW_PASSWORD(400, "비밀번호는 영문, 숫자, 특수문자를 모두 포함해 8자 이상 64자 이하로 입력해 주세요"),
	SAME_AS_CURRENT_PASSWORD(400, "새 비밀번호는 현재 비밀번호와 달라야 합니다"),

	EXPIRED_TOKEN(401, "인증 토큰이 만료되었습니다"),
	INVALID_TOKEN(401, "유효하지 않은 토큰입니다"),
	SESSION_EXPIRED(401, "세션이 만료되었습니다. 다시 로그인해 주세요"),
	SESSION_REVOKED(401, "로그아웃되었거나 무효화된 세션입니다. 다시 로그인해 주세요"),
	UNAUTHORIZED(401, "로그인이 필요합니다"),
	UNAUTHORIZED_PASSWORD_CHANGE(401, "비밀번호 변경 권한이 없습니다"),

	ACCOUNT_DISABLED(403, "비활성화된 계정입니다. 시스템 관리자에게 문의해 주세요"),
	PASSWORD_CHANGE_REQUIRED(403, "비밀번호를 변경한 뒤 이용할 수 있습니다"),

	USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다"),
	NOT_FOUND(404, "페이지를 찾을 수 없습니다"),

	ACCOUNT_LOCKED(423, "로그인에 5회 연속 실패해 계정이 잠겼습니다. 시스템 관리자에게 비밀번호 초기화를 요청해 주세요"),

	EXTERNAL_SERVICE_UNAVAILABLE(503, "외부 서비스에 일시적으로 연결할 수 없습니다"),

	JSON_PARSING_FAILURE(500, "JSON 매핑에 실패했습니다"),
	INTERNAL_SERVER_ERROR(500, "서버에서 오류가 발생했습니다");

	private final int status;
	private final String message;

	ErrorCode(final int status, final String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public String toString() {
		return message;
	}
}
