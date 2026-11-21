package org.wonik.coreviaai_be.domain.code.dto.response;

import org.wonik.coreviaai_be.domain.code.entity.CommonCode;

/**
 * 화면용 코드 항목. 표시 텍스트는 labelKey로 다국어 라벨(FR-ADM-06)에서 찾는다.
 */
public record CodeResponse(String code, String labelKey, int sortOrder) {

	public static CodeResponse from(final CommonCode code) {
		return new CodeResponse(code.getCode(), code.getLabelKey(), code.getSortOrder());
	}
}
