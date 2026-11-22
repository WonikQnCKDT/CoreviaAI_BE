package org.wonik.coreviaai_be.domain.wbs.service.command;

/**
 * 드래그 이동(순서·계층 변경).
 *
 * @param parentTaskId 옮길 곳의 상위 작업 ID. null이면 최상위 작업으로
 * @param index        옮길 곳 형제 목록(옮기는 항목을 뺀 목록)에서의 0부터 시작하는 위치. 형제 수와 같으면 맨 뒤
 * @param version      옮기는 항목의 version(낙관적 잠금)
 */
public record MoveWbsTaskCommand(Long parentTaskId, int index, long version) {
}
