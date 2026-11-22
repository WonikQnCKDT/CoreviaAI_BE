package org.wonik.coreviaai_be.domain.wbs.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.wonik.coreviaai_be.domain.wbs.entity.TaskDetails;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.service.command.UpdateWbsTaskCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * 작업·하위작업 기본 필드 수정. 모든 필드를 보낸 값으로 덮어쓴다(비운 설명·담당자·예상 공수는 지움).
 * 하위작업이 있는 작업의 기간은 하위작업 기간으로 산출되므로 조회한 값을 그대로 보내야 한다(다르면 400).
 * version은 조회 응답 값이며, 그 사이 다른 변경이 있었으면 409로 거부한다.
 */
public record UpdateWbsTaskRequest(
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
	BigDecimal estimatedEffort,

	@NotNull(message = "수정할 작업의 version을 입력해 주세요")
	@PositiveOrZero(message = "version은 0 이상이어야 합니다")
	Long version
) {

	public UpdateWbsTaskCommand toCommand() {
		return new UpdateWbsTaskCommand(new TaskDetails(name.strip(), DraftText.trimToNull(description),
			assigneeUserId, startDate, endDate, estimatedEffort), version);
	}
}
