package org.wonik.coreviaai_be.domain.project.entity.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 운영 방식(FR-PRJ-01 ②). MVP에서는 폭포수만 선택할 수 있고 애자일은 화면에 비활성으로만 표시한다.
 * 애자일(FR-PRJ-10) 구현 시 enabled만 바꿔 활성화한다.
 */
@Getter
@RequiredArgsConstructor
public enum Methodology {
	WATERFALL(true),
	AGILE(false);

	private final boolean enabled;
}
