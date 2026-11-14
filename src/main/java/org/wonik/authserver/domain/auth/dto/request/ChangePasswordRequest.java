package org.wonik.authserver.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 새 비밀번호 규칙(영문·숫자·특수문자 8~64자)은 PasswordPolicy에서 검사한다. 여기서는 형식 한도만 본다.
 */
public record ChangePasswordRequest(
	@NotBlank(message = "현재 비밀번호를 입력해 주세요")
	@Size(max = 128, message = "비밀번호가 너무 깁니다")
	String currentPassword,

	@NotBlank(message = "새 비밀번호를 입력해 주세요")
	@Size(max = 128, message = "비밀번호가 너무 깁니다")
	String newPassword
) {

	@Override
	public String toString() {
		return "ChangePasswordRequest[currentPassword=***, newPassword=***]";
	}
}
