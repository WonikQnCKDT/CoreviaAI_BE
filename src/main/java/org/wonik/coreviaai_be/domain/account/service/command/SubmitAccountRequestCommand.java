package org.wonik.coreviaai_be.domain.account.service.command;

public record SubmitAccountRequestCommand(
	String loginId,
	String name,
	Long departmentId,
	String purpose
) {
}
