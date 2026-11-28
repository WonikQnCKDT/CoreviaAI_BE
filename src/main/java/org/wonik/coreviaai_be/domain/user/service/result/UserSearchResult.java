package org.wonik.coreviaai_be.domain.user.service.result;

import org.wonik.global.user.entity.User;

/**
 * 이름 검색 결과 한 명.
 *
 * @param departmentName 소속 부서 이름. 부서가 없으면 null
 */
public record UserSearchResult(User user, String departmentName) {
}
