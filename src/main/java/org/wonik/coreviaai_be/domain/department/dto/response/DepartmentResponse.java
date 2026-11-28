package org.wonik.coreviaai_be.domain.department.dto.response;

import org.wonik.coreviaai_be.domain.department.entity.Department;

/**
 * 일반 사용자용 부서 항목(주관 부서 선택 등). 계층은 parentId로 표현하며 트리 구성은 FE에서 한다.
 *
 * @param parentId 상위 부서 ID. 최상위 부서면 null
 */
public record DepartmentResponse(Long id, Long parentId, String name) {

	public static DepartmentResponse from(final Department department) {
		return new DepartmentResponse(department.getId(), department.getParentId(), department.getName());
	}
}
