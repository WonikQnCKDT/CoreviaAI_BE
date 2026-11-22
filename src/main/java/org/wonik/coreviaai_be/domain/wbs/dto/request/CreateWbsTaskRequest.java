package org.wonik.coreviaai_be.domain.wbs.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.wonik.coreviaai_be.domain.wbs.entity.TaskDetails;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.service.command.CreateWbsTaskCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 작업·하위작업 등록. parentTaskId를 비우면 최상위 작업, 넣으면 그 작업의 하위작업이 된다.
 * 예상 공수는 인일, 소수 첫째 자리까지(NUMERIC(6,1)).
 */
public record CreateWbsTaskRequest(
	@Positive(message = "상위 작업 ID를 확인해 주세요")
	Long parentTaskId,

	@NotBlank(message = "작업명을 입력해 주세요")
	@Size(max = WbsTask.NAME_MAX_LENGTH, message = "작업명은 200자 이하로 입력해 주세요")
	String name,

	@Size(max = WbsTask.DESCRIPTION_MAX_LENGTH, message = "작업 설명은 2000자 이하로 입력해 주세요")
	String description,

	@Positive(message = "담당자 ID를 확인해 주세요")
	Long assigneeUserId,

	@NotNull(message = "시작일을 입력해 주세요")
	LocalDate startDate,

	@NotNull(message = "종료일을 입력해 주세요")
	LocalDate endDate,

	@DecimalMin(value = "0.1", message = "예상 공수는 0.1인일 이상이어야 합니다")
	@Digits(integer = 5, fraction = 1, message = "예상 공수는 99999.9인일 이하, 소수 첫째 자리까지 입력해 주세요")
	BigDecimal estimatedEffort
) {

	public CreateWbsTaskCommand toCommand() {
		return new CreateWbsTaskCommand(parentTaskId, new TaskDetails(name.strip(), DraftText.trimToNull(description),
			assigneeUserId, startDate, endDate, estimatedEffort));
	}
}
