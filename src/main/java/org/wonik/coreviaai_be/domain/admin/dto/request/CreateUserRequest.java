package org.wonik.coreviaai_be.domain.admin.dto.request;

import org.wonik.coreviaai_be.domain.admin.service.command.CreateUserCommand;
import org.wonik.global.common.type.SystemRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 사용자 추가 요청. 로그인 비밀번호는 이어서 관리자 비밀번호 초기화 API로 발급한다.
 * departmentId 필수 여부는 역할에 따라 달라 서비스에서 검증한다.
 */
public record CreateUserRequest(
	@NotBlank(message = "아이디를 입력해 주세요")
	@Size(max = 50, message = "아이디는 50자 이하로 입력해 주세요")
	@Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "아이디는 영문, 숫자, 마침표(.), 밑줄(_), 하이픈(-)만 사용할 수 있습니다")
	String loginId,

	@NotBlank(message = "이름을 입력해 주세요")
	@Size(max = 50, message = "이름은 50자 이하로 입력해 주세요")
	String name,

	@NotNull(message = "시스템 역할을 선택해 주세요")
	SystemRole systemRole,

	@Positive(message = "부서 ID를 확인해 주세요")
	Long departmentId,

	@Size(max = 50, message = "직무는 50자 이하로 입력해 주세요")
	String jobTitle,

	Boolean canCreateProject
) {

	/** canCreateProject를 생략하면 권한 없음으로 본다. */
	public CreateUserCommand toCommand() {
		return new CreateUserCommand(
			loginId,
			name,
			systemRole,
			departmentId,
			jobTitle,
			Boolean.TRUE.equals(canCreateProject)
		);
	}
}
