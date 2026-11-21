package org.wonik.coreviaai_be.domain.admin.dto.request;

import org.wonik.coreviaai_be.domain.code.entity.CommonCode;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 코드 추가. 라벨 키는 규칙(code.그룹.코드)대로 서버가 정하므로 받지 않는다. sortOrder가 없으면 그룹의 마지막 순서 뒤에 둔다.
 */
public record CreateCodeRequest(
	@NotNull(message = "코드 값을 입력해 주세요")
	@Pattern(regexp = CommonCode.CODE_PATTERN, message = "코드 값은 영문 대문자로 시작하는 대문자·숫자·밑줄 40자 이하로 입력해 주세요")
	String code,

	@Min(value = 0, message = "정렬 순서는 0 이상 9999 이하로 입력해 주세요")
	@Max(value = CommonCode.MAX_SORT_ORDER, message = "정렬 순서는 0 이상 9999 이하로 입력해 주세요")
	Integer sortOrder
) {
}
