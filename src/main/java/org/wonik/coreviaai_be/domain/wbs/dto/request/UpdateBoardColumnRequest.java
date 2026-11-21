package org.wonik.coreviaai_be.domain.wbs.dto.request;

import org.wonik.coreviaai_be.domain.wbs.entity.BoardColumn;
import org.wonik.coreviaai_be.domain.wbs.service.command.UpdateBoardColumnCommand;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 컬럼 이름·WIP 한도 변경. 두 값을 모두 보낸 값으로 덮어쓴다: 이름을 비우면 기본 이름으로 되돌리고, WIP 한도를 비우면 한도를 없앤다.
 */
public record UpdateBoardColumnRequest(
	@Size(max = BoardColumn.NAME_MAX_LENGTH, message = "컬럼 이름은 50자 이하로 입력해 주세요")
	String name,

	@Min(value = 1, message = "WIP 한도는 1 이상이어야 합니다")
	@Max(value = BoardColumn.MAX_WIP_LIMIT, message = "WIP 한도는 999 이하여야 합니다")
	Integer wipLimit
) {

	public UpdateBoardColumnCommand toCommand() {
		return new UpdateBoardColumnCommand(DraftText.trimToNull(name), wipLimit);
	}
}
