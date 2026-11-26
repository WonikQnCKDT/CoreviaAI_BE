package org.wonik.coreviaai_be.domain.account.dto.request;

import org.wonik.coreviaai_be.domain.account.service.command.ApproveAccountRequestCommand;
import org.wonik.global.common.type.SystemRole;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ApproveAccountRequest(
	@NotNull(message = "신청 버전을 입력해 주세요")
	@Min(value = 0, message = "신청 버전을 확인해 주세요")
	Long version,

	@NotNull(message = "시스템 역할을 선택해 주세요")
	SystemRole systemRole,

	@Positive(message = "부서 ID를 확인해 주세요")
	Long departmentId,

	@Size(max = 50, message = "직무는 50자 이하로 입력해 주세요")
	String jobTitle
) {

	public ApproveAccountRequestCommand toCommand() {
		return new ApproveAccountRequestCommand(version, systemRole, departmentId, jobTitle);
	}
}
