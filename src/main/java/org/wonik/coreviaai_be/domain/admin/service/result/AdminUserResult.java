package org.wonik.coreviaai_be.domain.admin.service.result;

import org.wonik.global.user.entity.User;

/**
 * 관리자 사용자 조회·변경 결과.
 *
 * @param departmentName 소속 부서 이름. 부서가 없으면 null
 */
public record AdminUserResult(User user, String departmentName) {
}
