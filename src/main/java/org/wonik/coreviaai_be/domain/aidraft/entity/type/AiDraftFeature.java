package org.wonik.coreviaai_be.domain.aidraft.entity.type;

/**
 * ai_drafts를 쓰는 AI 기능. 값을 추가하면 ck_ai_drafts_feature도 새 마이그레이션으로 바꾼다.
 */
public enum AiDraftFeature {
	/** AI WBS 초안(FR-PRJ-07) */
	WBS_DRAFT,
	/** 업무 구체화 초안(FR-TSK-11) */
	TASK_DETAIL,
	/** 업무일지 요약(FR-PSN-04) */
	WORKLOG_SUMMARY,
	/** 회의록 정리(FR-DOC-02) */
	MEETING_SUMMARY,
	/** 주간 리포트 초안(FR-RPT-01) */
	WEEKLY_REPORT,
	/** 진척률 추정(FR-AIR-14) */
	PROGRESS_ESTIMATE,
	/** 할당 공수 추정(FR-AIR-15) */
	EFFORT_ESTIMATE
}
