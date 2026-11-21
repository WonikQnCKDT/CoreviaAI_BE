package org.wonik.coreviaai_be.domain.project.entity.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 운영 방식(FR-PRJ-01 ②). 방법론을 강제하지 않도록 단계 없음·폭포수·애자일 중 고른다.
 * <ul>
 *   <li>NONE: 단계·게이트 없이 업무 목록으로 운영(빈 템플릿 기본값)</li>
 *   <li>WATERFALL: 기본 5단계 WBS와 단계 게이트(FR-PRJ-05)</li>
 *   <li>AGILE: 백로그·스프린트(FR-PRJ-10, C). MVP에서는 화면에 비활성으로만 표시한다</li>
 * </ul>
 * 애자일 구현 시 enabled만 바꿔 활성화한다.
 */
@Getter
@RequiredArgsConstructor
public enum Methodology {
	NONE(true, false),
	WATERFALL(true, true),
	AGILE(false, false);

	private final boolean enabled;
	/** 단계·게이트를 쓰는지. 게이트 승인자와 산출물·게이트 모듈은 이 값이 true일 때만 의미가 있다. */
	private final boolean usesPhases;
}
