package org.wonik.coreviaai_be.domain.wbs.dto.request;

import org.wonik.coreviaai_be.domain.wbs.entity.BoardColumn;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;
import org.wonik.coreviaai_be.domain.wbs.service.command.CreateBoardColumnCommand;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 컬럼 추가. 이름을 비우면 기본 이름(상태 코드 라벨)을 쓴다. 매핑 상태는 만든 뒤 바꿀 수 없다.
 */
public record CreateBoardColumnRequest(
	@Size(max = BoardColumn.NAME_MAX_LENGTH, message = "컬럼 이름은 50자 이하로 입력해 주세요")
	@Pattern(regexp = BoardColumn.NAME_PATTERN, message = "컬럼 이름에는 줄바꿈·탭 같은 제어 문자를 쓸 수 없습니다")
	String name,

	@NotNull(message = "컬럼의 상태를 선택해 주세요")
	WbsTaskStatus statusCategory,

	@Min(value = 1, message = "WIP 한도는 1 이상이어야 합니다")
	@Max(value = BoardColumn.MAX_WIP_LIMIT, message = "WIP 한도는 999 이하여야 합니다")
	Integer wipLimit
) {

	public CreateBoardColumnCommand toCommand() {
		return new CreateBoardColumnCommand(DraftText.trimToNull(name), statusCategory, wipLimit);
	}
}
