package org.wonik.coreviaai_be.domain.wbs.service.command;

import java.util.List;

/**
 * AI WBS 초안 생성 입력(FR-PRJ-07). 목적·요구사항 중 하나 이상은 값이 있다(요청 검증).
 * 빈 문자열은 null로 정규화되어 들어온다.
 */
public record GenerateWbsDraftCommand(
	String projectPurpose,
	String requirementsText,
	List<String> constraints,
	String userInstruction
) {
}
