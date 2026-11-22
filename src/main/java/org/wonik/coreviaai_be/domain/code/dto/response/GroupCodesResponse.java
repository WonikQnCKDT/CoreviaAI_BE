package org.wonik.coreviaai_be.domain.code.dto.response;

import java.util.List;

import org.wonik.coreviaai_be.domain.code.service.result.GroupCodes;

/**
 * 화면용 그룹별 코드 목록(사용 중 코드만, 정렬 순서 오름차순).
 */
public record GroupCodesResponse(String groupCode, String labelKey, List<CodeResponse> codes) {

	public static GroupCodesResponse from(final GroupCodes result) {
		return new GroupCodesResponse(
			result.group().getGroupCode(),
			result.group().getLabelKey(),
			result.codes().stream().map(CodeResponse::from).toList()
		);
	}
}
