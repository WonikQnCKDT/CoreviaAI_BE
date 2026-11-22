package org.wonik.coreviaai_be.domain.wbs.dto.request;

import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.service.BoardColumnService;
import org.wonik.coreviaai_be.domain.wbs.service.command.ReorderBoardColumnsCommand;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 컬럼 순서 변경. 보드의 모든 컬럼 ID를 새 순서대로 한 번씩 보낸다. */
public record ReorderBoardColumnsRequest(
	@NotEmpty(message = "컬럼 순서를 입력해 주세요")
	@Size(max = BoardColumnService.MAX_COLUMNS_PER_PROJECT, message = "컬럼은 최대 20개입니다")
	List<@NotNull(message = "컬럼 ID를 확인해 주세요") @Positive(message = "컬럼 ID를 확인해 주세요") Long> columnIds
) {

	public ReorderBoardColumnsCommand toCommand() {
		return new ReorderBoardColumnsCommand(List.copyOf(columnIds));
	}
}
