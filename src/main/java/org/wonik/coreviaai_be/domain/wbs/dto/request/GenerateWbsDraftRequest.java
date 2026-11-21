package org.wonik.coreviaai_be.domain.wbs.dto.request;

import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.service.command.GenerateWbsDraftCommand;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AI WBS 초안 생성 요청(FR-PRJ-07). 프로젝트 목적과 요구사항 문서 텍스트 중 하나 이상이 필요하다.
 * 길이 상한은 AI 입력 크기를 제한하기 위한 값이다(02. AI 생성 API 미결 사항 6: 입력 크기, 팀 확정 전 초안).
 */
public record GenerateWbsDraftRequest(
	@Size(max = 2000, message = "프로젝트 목적은 2000자 이하로 입력해 주세요")
	String projectPurpose,

	@Size(max = 20000, message = "요구사항은 20000자 이하로 입력해 주세요")
	String requirementsText,

	@Size(max = 20, message = "제약조건은 최대 20개까지 입력할 수 있습니다")
	List<@NotBlank(message = "제약조건을 입력해 주세요") @Size(max = 200, message = "제약조건은 200자 이하로 입력해 주세요") String> constraints,

	@Size(max = 1000, message = "추가 지시는 1000자 이하로 입력해 주세요")
	String userInstruction
) {

	@AssertTrue(message = "프로젝트 목적이나 요구사항 중 하나 이상 입력해 주세요")
	public boolean isPurposeOrRequirementsPresent() {
		return hasText(projectPurpose) || hasText(requirementsText);
	}

	public GenerateWbsDraftCommand toCommand() {
		return new GenerateWbsDraftCommand(
			trimToNull(projectPurpose),
			trimToNull(requirementsText),
			constraints == null ? List.of() : constraints.stream().map(String::strip).toList(),
			trimToNull(userInstruction)
		);
	}

	private static boolean hasText(final String value) {
		return value != null && !value.isBlank();
	}

	private static String trimToNull(final String value) {
		return hasText(value) ? value.strip() : null;
	}
}
