package org.wonik.coreviaai_be.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 부서 이름 변경·상위 부서 이동. 보낸 값으로 덮어쓴다(parentId가 null이면 최상위로 이동).
 */
public record UpdateDepartmentRequest(
	@NotBlank(message = "부서 이름을 입력해 주세요")
	@Size(max = 50, message = "부서 이름은 50자 이하로 입력해 주세요")
	String name,

	@Positive(message = "상위 부서 ID를 확인해 주세요")
	Long parentId
) {
}
