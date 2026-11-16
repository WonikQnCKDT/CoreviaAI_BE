package org.wonik.coreviaai_be.domain.admin.dto.request;

import org.wonik.coreviaai_be.domain.project.dto.request.CreateProjectRequest;
import org.wonik.coreviaai_be.domain.project.service.command.ReassignPmCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * 시스템 관리자의 PM 재지정(FR-PRJ-04). 인사 발령으로 정해진 새 PM을 프로젝트 코드와 사용자 ID만으로 배치한다.
 * 코드는 생성 때와 같은 형식이며 대문자로 바꿔 찾는다.
 */
public record ReassignPmRequest(
	@NotBlank(message = "프로젝트 코드를 입력해 주세요")
	@Pattern(regexp = CreateProjectRequest.CODE_PATTERN, message = "프로젝트 코드 형식을 확인해 주세요")
	String projectCode,

	@NotNull(message = "새 PM을 입력해 주세요")
	@Positive(message = "사용자 ID를 확인해 주세요")
	Long newPmUserId
) {

	public ReassignPmCommand toCommand() {
		return new ReassignPmCommand(CreateProjectRequest.normalizeCode(projectCode), newPmUserId);
	}
}
