package org.wonik.coreviaai_be.domain.wbs.service.result;

import java.util.List;

/**
 * WBS 변경 결과. 이동·하위작업 변경은 다른 항목의 순서·상위 기간·version도 바꾸므로 바뀐 뒤의 트리 전체를 함께 돌려준다.
 *
 * @param taskId 등록·수정·이동·삭제한 항목 ID
 */
public record WbsTaskChange(Long taskId, List<WbsTreeNode> tasks) {
}
