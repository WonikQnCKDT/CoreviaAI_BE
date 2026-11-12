package org.wonik.coreviaai_be.domain.admin.service.result;

import org.wonik.global.department.entity.Department;

/**
 * 관리자 부서 조회·변경 결과.
 *
 * @param activeMemberCount 이 부서에 직접 소속된 활성 사용자 수(하위 부서 인원 제외)
 */
public record AdminDepartmentResult(Department department, long activeMemberCount) {
}
