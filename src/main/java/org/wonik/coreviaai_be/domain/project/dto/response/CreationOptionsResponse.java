package org.wonik.coreviaai_be.domain.project.dto.response;

import java.util.Arrays;
import java.util.List;

import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;

/**
 * 생성 화면 ②·③의 선택지. MVP 범위(폭포수·SW 액터)를 FE가 하드코딩하지 않고 서버 enum 설정을 그대로 따르게 한다.
 * 애자일·제조 모듈을 열 때 서버만 바꾸면 화면이 함께 바뀐다.
 */
public record CreationOptionsResponse(
	List<MethodologyOptionResponse> methodologies,
	List<DepartmentTypeOptionResponse> departmentTypes
) {

	public static CreationOptionsResponse current() {
		return new CreationOptionsResponse(
			Arrays.stream(Methodology.values()).map(MethodologyOptionResponse::from).toList(),
			Arrays.stream(DepartmentType.values()).map(DepartmentTypeOptionResponse::from).toList()
		);
	}
}
