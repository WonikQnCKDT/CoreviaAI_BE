package org.wonik.coreviaai_be.domain.admin.dto.response;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.admin.service.result.AdminUserCreatedResult;
import org.wonik.coreviaai_be.domain.admin.service.result.AdminUserResult;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.entity.UserStatus;

/**
 * 관리자 사용자 생성 응답. 생성 직후 한 번만 임시 비밀번호를 함께 돌려준다.
 * 조회·수정 응답({@link AdminUserResponse})에는 임시 비밀번호를 포함하지 않는다.
 */
public record AdminUserCreatedResponse(
	Long id,
	String loginId,
	String name,
	SystemRole systemRole,
	UserStatus status,
	Long departmentId,
	String departmentName,
	String jobTitle,
	Instant lastLoginAt,
	Instant createdAt,
	Instant updatedAt,
	String temporaryPassword
) {

	public static AdminUserCreatedResponse from(final AdminUserCreatedResult result) {
		final AdminUserResult userResult = result.user();
		final User user = userResult.user();
		return new AdminUserCreatedResponse(
			user.getId(),
			user.getLoginId(),
			user.getName(),
			user.getSystemRole(),
			user.getStatus(),
			user.getDepartmentId(),
			userResult.departmentName(),
			user.getJobTitle(),
			user.getLastLoginAt(),
			user.getCreatedAt(),
			user.getUpdatedAt(),
			result.temporaryPassword()
		);
	}

	/** 로그·예외 메시지에 임시 비밀번호가 찍히지 않도록 가린다. */
	@Override
	public String toString() {
		return "AdminUserCreatedResponse[id=" + id + ", loginId=" + loginId + ", temporaryPassword=***]";
	}
}
