package org.wonik.coreviaai_be.domain.wbs.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftTaskCommand;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 반영할 작업 한 개. 요청 순서가 표시 순서다. phaseId는 AI 제안 단계 또는 PM이 검토 화면에서 바꾼 단계다.
 * 기간은 프로젝트 기간이 있으면 그 안이어야 하고(서비스 검증), 예상 공수는 인일 단위 소수 첫째 자리까지다.
 */
public record ApplyDraftTaskRequest(
	@Positive(message = "단계 ID를 확인해 주세요")
	Long phaseId,

	@NotBlank(message = "작업의 임시 ID가 필요합니다")
	@Size(max = 50, message = "임시 ID는 50자 이하여야 합니다")
	String tempId,

	@NotBlank(message = "작업명을 입력해 주세요")
	@Size(max = WbsTask.NAME_MAX_LENGTH, message = "작업명은 200자 이하로 입력해 주세요")
	String name,

	@Size(max = WbsTask.DESCRIPTION_MAX_LENGTH, message = "작업 설명은 2000자 이하로 입력해 주세요")
	String description,

	@NotNull(message = "작업 시작일을 입력해 주세요")
	LocalDate startDate,

	@NotNull(message = "작업 종료일을 입력해 주세요")
	LocalDate endDate,

	@DecimalMin(value = "0.1", message = "예상 공수는 0.1인일 이상이어야 합니다")
	@Digits(integer = 5, fraction = 1, message = "예상 공수는 소수 첫째 자리까지 입력해 주세요")
	BigDecimal estimatedPersonDays,

	@Positive(message = "담당자 ID를 확인해 주세요")
	Long assigneeUserId,

	@Size(max = 50, message = "선행 작업은 최대 50개까지 지정할 수 있습니다")
	List<@NotBlank(message = "선행 작업의 임시 ID를 확인해 주세요") String> dependencies,

	@Size(max = 50, message = "하위작업은 작업당 최대 50개입니다")
	List<@NotNull(message = "하위작업 값을 확인해 주세요") @Valid ApplyDraftSubtaskRequest> subtasks
) {

	public DraftTaskCommand toCommand() {
		return new DraftTaskCommand(
			phaseId,
			tempId,
			name.strip(),
			DraftText.trimToNull(description),
			startDate,
			endDate,
			estimatedPersonDays,
			assigneeUserId,
			dependencies == null ? List.of() : dependencies,
			subtasks == null ? List.of() : subtasks.stream().map(ApplyDraftSubtaskRequest::toCommand).toList()
		);
	}
}
