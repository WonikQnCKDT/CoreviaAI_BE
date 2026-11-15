package org.wonik.coreviaai_be.domain.project.entity.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 액터 목록을 묶는 부서 유형(FR-PRJ-01 ③, 요구사항 명세서 3.2). MVP에서는 SW만 선택할 수 있다.
 * 제조 확장 모듈(4.14) 구현 시 MANUFACTURING의 enabled만 바꿔 활성화한다.
 */
@Getter
@RequiredArgsConstructor
public enum DepartmentType {
	SW(true),
	MANUFACTURING(false);

	private final boolean enabled;
}
