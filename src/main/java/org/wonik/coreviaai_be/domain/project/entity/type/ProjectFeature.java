package org.wonik.coreviaai_be.domain.project.entity.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 기능 모듈(FR-PRJ-06). 생성 시(FR-PRJ-01 ④) 사용 여부를 고르고, 사용하지 않는 모듈은 프로젝트 메뉴에 표시하지 않는다.
 * GIT은 FR-DEV-01(S) 구현 시 enabled를 바꿔 선택할 수 있게 한다.
 */
@Getter
@RequiredArgsConstructor
public enum ProjectFeature {
	WBS(true, false),
	KANBAN(true, false),
	BOARD(true, false),
	MEETING(true, false),
	/** 산출물·게이트. 단계가 있는 운영 방식(폭포수)에서만 쓸 수 있다. */
	DELIVERABLE(true, true),
	ISSUE(true, false),
	FILE(true, false),
	GIT(false, false);

	private final boolean enabled;
	private final boolean requiresPhases;

	/** 이 모듈을 해당 운영 방식의 프로젝트에서 켤 수 있는지. */
	public boolean selectableFor(final Methodology methodology) {
		return enabled && (!requiresPhases || methodology.isUsesPhases());
	}
}
