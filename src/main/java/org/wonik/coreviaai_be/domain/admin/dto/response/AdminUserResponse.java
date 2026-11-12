package org.wonik.coreviaai_be.domain.admin.dto.response;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.admin.service.result.AdminUserResult;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.entity.UserStatus;

/**
 * 관리자 사용자 목록·상세 응답 항목.
 *
 * @param departmentName 소속 부서 이름. 부서가 없으면 null
 */
public record AdminUserResponse(
	Long id,
	String loginId,
	String name,
	SystemRole systemRole,
	UserStatus status,
	Long departmentId,
	String departmentName,
	String jobTitle,
	boolean canCreateProject,
	Instant lastLoginAt,
	Instant createdAt,
	Instant updatedAt
) {

	public static AdminUserResponse from(final AdminUserResult result) {
		final User user = result.user();
		return new AdminUserResponse(
			user.getId(),
			user.getLoginId(),
			user.getName(),
			user.getSystemRole(),
			user.getStatus(),
			user.getDepartmentId(),
			result.departmentName(),
			user.getJobTitle(),
			user.isCanCreateProject(),
			user.getLastLoginAt(),
			user.getCreatedAt(),
			user.getUpdatedAt()
		);
	}
}
