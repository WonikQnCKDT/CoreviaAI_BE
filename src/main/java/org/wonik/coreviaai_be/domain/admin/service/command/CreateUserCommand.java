package org.wonik.coreviaai_be.domain.admin.service.command;

import org.wonik.global.common.type.SystemRole;

/**
 * 사용자 추가 입력. 형식 검증은 요청 경계(CreateUserRequest)에서 끝난 값이다.
 *
 * @param departmentId 시스템 관리자가 아니면 필수(서비스에서 검증)
 */
public record CreateUserCommand(
	String loginId,
	String name,
	SystemRole systemRole,
	Long departmentId,
	String jobTitle,
	boolean canCreateProject
) {
}
