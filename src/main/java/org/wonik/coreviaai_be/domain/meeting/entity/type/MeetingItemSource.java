package org.wonik.coreviaai_be.domain.meeting.entity.type;

/** 회의록 결정사항·액션아이템의 작성 출처. AI 값은 사람이 수락한 뒤에만 저장한다. */
public enum MeetingItemSource {
	MANUAL,
	AI
}
