package org.wonik.coreviaai_be.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 부서 추가. parentId가 없으면 최상위 부서로 만든다.
 */
public record CreateDepartmentRequest(
	@NotBlank(message = "부서 이름을 입력해 주세요")
	@Size(max = 50, message = "부서 이름은 50자 이하로 입력해 주세요")
	String name,

	@Positive(message = "상위 부서 ID를 확인해 주세요")
	Long parentId
) {
}
