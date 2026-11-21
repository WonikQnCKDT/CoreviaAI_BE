package org.wonik.coreviaai_be.domain.admin.dto.request;

import org.wonik.coreviaai_be.domain.code.entity.CommonCode;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 코드 정렬 순서·사용 여부 변경. 코드 값과 라벨 키는 고정이라 받지 않는다(본문에 넣어도 무시된다).
 */
public record UpdateCodeRequest(
	@NotNull(message = "정렬 순서를 입력해 주세요")
	@Min(value = 0, message = "정렬 순서는 0 이상 9999 이하로 입력해 주세요")
	@Max(value = CommonCode.MAX_SORT_ORDER, message = "정렬 순서는 0 이상 9999 이하로 입력해 주세요")
	Integer sortOrder,

	@NotNull(message = "사용 여부를 선택해 주세요")
	Boolean active
) {
}
