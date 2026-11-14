package org.wonik.authserver.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
	@NotBlank(message = "아이디를 입력해 주세요")
	@Size(max = 50, message = "아이디는 50자 이하로 입력해 주세요")
	String loginId,

	@NotBlank(message = "비밀번호를 입력해 주세요")
	@Size(max = 128, message = "비밀번호가 너무 깁니다")
	String password
) {

	/** 요청 객체가 로그에 찍혀도 비밀번호가 남지 않게 한다. */
	@Override
	public String toString() {
		return "LoginRequest[loginId=" + loginId + ", password=***]";
	}
}
