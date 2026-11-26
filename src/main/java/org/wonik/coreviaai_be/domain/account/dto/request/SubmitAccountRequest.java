package org.wonik.coreviaai_be.domain.account.dto.request;

import org.wonik.coreviaai_be.domain.account.service.command.SubmitAccountRequestCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SubmitAccountRequest(
	@NotBlank(message = "아이디를 입력해 주세요")
	@Size(max = 50, message = "아이디는 50자 이하로 입력해 주세요")
	@Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "아이디는 영문, 숫자, 마침표(.), 밑줄(_), 하이픈(-)만 사용할 수 있습니다")
	String loginId,

	@NotBlank(message = "이름을 입력해 주세요")
	@Size(max = 50, message = "이름은 50자 이하로 입력해 주세요")
	String name,

	@Positive(message = "부서 ID를 확인해 주세요")
	Long departmentId,

	@NotBlank(message = "사용 목적을 입력해 주세요")
	@Size(max = 500, message = "사용 목적은 500자 이하로 입력해 주세요")
	String purpose
) {

	public SubmitAccountRequestCommand toCommand() {
		return new SubmitAccountRequestCommand(loginId, name, departmentId, purpose);
	}
}
