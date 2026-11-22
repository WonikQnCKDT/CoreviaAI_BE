package org.wonik.coreviaai_be.domain.wbs.dto.request;

import org.wonik.coreviaai_be.domain.wbs.service.command.MoveBoardCardCommand;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * 카드를 다른 컬럼으로 옮긴다(드래그). 카드 상태는 옮긴 컬럼의 상태가 된다.
 * version은 보드·업무 상세·WBS 트리 응답의 업무 version이며, 그 사이 다른 변경이 있었으면 409.
 */
public record MoveBoardCardRequest(
	@NotNull(message = "옮길 컬럼을 선택해 주세요")
	@Positive(message = "컬럼 ID를 확인해 주세요")
	Long columnId,

	@NotNull(message = "옮길 업무의 version을 입력해 주세요")
	@PositiveOrZero(message = "version은 0 이상이어야 합니다")
	Long version
) {

	public MoveBoardCardCommand toCommand() {
		return new MoveBoardCardCommand(columnId, version);
	}
}
