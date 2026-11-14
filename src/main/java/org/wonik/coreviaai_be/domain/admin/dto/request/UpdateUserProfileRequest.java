package org.wonik.coreviaai_be.domain.admin.dto.request;

import org.wonik.coreviaai_be.domain.admin.service.command.UpdateUserProfileCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 이름·부서·직무 수정. 보낸 값으로 덮어쓴다(departmentId·jobTitle이 null이면 비움).
 */
public record UpdateUserProfileRequest(
	@NotBlank(message = "이름을 입력해 주세요")
	@Size(max = 50, message = "이름은 50자 이하로 입력해 주세요")
	String name,

	@Positive(message = "부서 ID를 확인해 주세요")
	Long departmentId,

	@Size(max = 50, message = "직무는 50자 이하로 입력해 주세요")
	String jobTitle
) {

	public UpdateUserProfileCommand toCommand() {
		return new UpdateUserProfileCommand(name, departmentId, jobTitle);
	}
}
