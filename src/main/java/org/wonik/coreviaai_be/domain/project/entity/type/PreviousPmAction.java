package org.wonik.coreviaai_be.domain.project.entity.type;

/**
 * PM 위임 후 기존 PM의 처리(FR-PRJ-04): 남은 지정 액터 중 하나로 역할을 바꾸거나 멤버에서 빠진다.
 */
public enum PreviousPmAction {
	CHANGE_ROLE,
	LEAVE
}
