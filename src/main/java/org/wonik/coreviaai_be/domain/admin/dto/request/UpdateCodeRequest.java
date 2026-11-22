package org.wonik.coreviaai_be.domain.admin.dto.request;

import org.wonik.coreviaai_be.domain.code.entity.CommonCode;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 코드 정렬 순서 변경. 코드 값·라벨 키·사용 여부는 관리자가 바꾸지 않으므로 받지 않는다(본문에 넣어도 무시된다).
 * 사용 여부(active)는 마이그레이션으로만 바꾼다(FR-ADM-07: 코드 값은 고정, 표시 텍스트만 수정).
 */
public record UpdateCodeRequest(
	@NotNull(message = "정렬 순서를 입력해 주세요")
	@Min(value = 0, message = "정렬 순서는 0 이상 9999 이하로 입력해 주세요")
	@Max(value = CommonCode.MAX_SORT_ORDER, message = "정렬 순서는 0 이상 9999 이하로 입력해 주세요")
	Integer sortOrder
) {
}
