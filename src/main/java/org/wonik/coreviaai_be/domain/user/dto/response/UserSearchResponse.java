package org.wonik.coreviaai_be.domain.user.dto.response;

import org.wonik.coreviaai_be.domain.user.service.result.UserSearchResult;

/**
 * 일반 사용자용 이름 검색 응답 항목. 초대 대상을 고르는 데 필요한 값만 둔다(아이디·이메일·역할·상태는 노출하지 않는다).
 *
 * @param departmentName 소속 부서 이름. 부서가 없으면 null
 * @param jobTitle 직무. 입력하지 않았으면 null
 */
public record UserSearchResponse(Long id, String name, String departmentName, String jobTitle) {

	public static UserSearchResponse from(final UserSearchResult result) {
		return new UserSearchResponse(
			result.user().getId(),
			result.user().getName(),
			result.departmentName(),
			result.user().getJobTitle()
		);
	}
}
