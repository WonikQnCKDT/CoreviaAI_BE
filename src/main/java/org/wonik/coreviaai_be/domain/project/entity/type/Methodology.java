package org.wonik.coreviaai_be.domain.project.entity.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 운영 방식(FR-PRJ-01 ②). 단계 구성과 독립적으로 방법론 없음·폭포수·애자일 중 고른다.
 * <ul>
 *   <li>NONE: 특정 방법론을 지정하지 않음(빈 템플릿 기본값)</li>
 *   <li>WATERFALL: 폭포수 방식. IT 템플릿은 기본 5단계를 제안하지만 생성 전에 바꿀 수 있음</li>
 *   <li>AGILE: 백로그·스프린트(FR-PRJ-10, C). MVP에서는 화면에 비활성으로만 표시한다</li>
 * </ul>
 * 애자일 구현 시 enabled만 바꿔 활성화한다.
 */
@Getter
@RequiredArgsConstructor
public enum Methodology {
	NONE(true),
	WATERFALL(true),
	AGILE(false);

	private final boolean enabled;
}
