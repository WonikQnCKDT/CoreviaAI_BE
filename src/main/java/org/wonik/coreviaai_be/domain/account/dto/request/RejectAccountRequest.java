package org.wonik.coreviaai_be.domain.account.dto.request;

import org.wonik.coreviaai_be.domain.account.service.command.RejectAccountRequestCommand;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RejectAccountRequest(
	@NotNull(message = "신청 버전을 입력해 주세요")
	@Min(value = 0, message = "신청 버전을 확인해 주세요")
	Long version,

	@NotBlank(message = "반려 사유를 입력해 주세요")
	@Size(max = 500, message = "반려 사유는 500자 이하로 입력해 주세요")
	String rejectReason
) {

	public RejectAccountRequestCommand toCommand() {
		return new RejectAccountRequestCommand(version, rejectReason);
	}
}
