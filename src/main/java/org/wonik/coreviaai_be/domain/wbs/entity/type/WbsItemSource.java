package org.wonik.coreviaai_be.domain.wbs.entity.type;

/**
 * WBS 항목(작업·마일스톤)을 누가 만들었는지. AI_DRAFT는 PM이 승인한 AI 초안 항목이다(FR-PRJ-07, NFR-10 출처 표시).
 */
public enum WbsItemSource {
	MANUAL,
	AI_DRAFT
}
