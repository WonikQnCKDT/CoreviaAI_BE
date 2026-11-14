package org.wonik.coreviaai_be.domain.admin.dto.response;

import java.time.Instant;

import org.wonik.coreviaai_be.domain.admin.service.result.AdminDepartmentResult;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.coreviaai_be.domain.department.entity.DepartmentStatus;

/**
 * 관리자 부서 응답 항목. 계층은 parentId로 표현하며 트리 구성은 FE에서 한다.
 *
 * @param activeMemberCount 이 부서에 직접 소속된 활성 사용자 수(하위 부서 인원 제외)
 */
public record AdminDepartmentResponse(
	Long id,
	Long parentId,
	String name,
	DepartmentStatus status,
	long activeMemberCount,
	Instant createdAt,
	Instant updatedAt
) {

	public static AdminDepartmentResponse from(final AdminDepartmentResult result) {
		final Department department = result.department();
		return new AdminDepartmentResponse(
			department.getId(),
			department.getParentId(),
			department.getName(),
			department.getStatus(),
			result.activeMemberCount(),
			department.getCreatedAt(),
			department.getUpdatedAt()
		);
	}
}
