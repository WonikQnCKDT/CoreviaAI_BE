package org.wonik.coreviaai_be.domain.account.service.command;

import org.wonik.global.common.type.SystemRole;

/**
 * 승인 시 역할·소속을 확정한다. 직무는 선택이다. 접근 범위는 시스템 역할로 정하고,
 * 프로젝트 참여는 이후 멤버 초대(FR-PRJ-04)에서 정한다.
 */
public record ApproveAccountRequestCommand(
	long version,
	SystemRole systemRole,
	Long departmentId,
	String jobTitle
) {
}
