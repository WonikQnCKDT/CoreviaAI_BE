package org.wonik.coreviaai_be.domain.wbs.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftSubtaskCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 반영할 하위작업 한 개. 기간을 비우면 상위 작업 기간을 쓴다(AI 초안의 하위작업에는 기간이 없음).
 */
public record ApplyDraftSubtaskRequest(
	@NotBlank(message = "하위작업의 임시 ID가 필요합니다")
	@Size(max = 50, message = "임시 ID는 50자 이하여야 합니다")
	String tempId,

	@NotBlank(message = "하위작업명을 입력해 주세요")
	@Size(max = WbsTask.NAME_MAX_LENGTH, message = "하위작업명은 200자 이하로 입력해 주세요")
	String name,

	@Size(max = WbsTask.DESCRIPTION_MAX_LENGTH, message = "하위작업 설명은 2000자 이하로 입력해 주세요")
	String description,

	LocalDate startDate,

	LocalDate endDate,

	@DecimalMin(value = "0.1", message = "예상 공수는 0.1인일 이상이어야 합니다")
	@Digits(integer = 5, fraction = 1, message = "예상 공수는 소수 첫째 자리까지 입력해 주세요")
	BigDecimal estimatedPersonDays,

	@Positive(message = "담당자 ID를 확인해 주세요")
	Long assigneeUserId
) {

	public DraftSubtaskCommand toCommand() {
		return new DraftSubtaskCommand(tempId, name.strip(), DraftText.trimToNull(description), startDate, endDate,
			estimatedPersonDays, assigneeUserId);
	}
}
