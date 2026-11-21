package org.wonik.aiserver.domain.wbs.api;

import java.time.LocalDate;
import java.util.List;

/**
 * WBS 초안 요청의 projectContext. 기간은 YYYY-MM-DD이고 프로젝트 기간이 선택 입력이라(FR-PRJ-01) null일 수 있다.
 * methodology는 WATERFALL, 제약조건이 없으면 빈 배열이다.
 */
public record AiWbsProjectContext(LocalDate startDate, LocalDate endDate, String methodology, List<String> constraints) {
}
