package org.wonik.coreviaai_be.domain.wbs.dto.response;

import java.util.List;

import org.wonik.coreviaai_be.domain.wbs.service.result.WbsTaskChange;

/**
 * 등록·수정·이동·삭제 결과. taskId는 대상 항목 ID, tasks는 바뀐 뒤의 WBS 전체다(다른 항목의 순서·상위 기간·version이
 * 함께 바뀔 수 있어 화면은 이 값으로 트리를 다시 그린다).
 */
public record WbsTaskChangeResponse(Long taskId, List<WbsTaskResponse> tasks) {

	public static WbsTaskChangeResponse from(final WbsTaskChange change) {
		return new WbsTaskChangeResponse(change.taskId(), WbsTaskResponse.listOf(change.tasks()));
	}
}
