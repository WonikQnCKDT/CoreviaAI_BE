package org.wonik.coreviaai_be.domain.wbs.service.command;

/**
 * 컬럼 이름·WIP 한도를 보낸 값으로 덮어쓴다.
 *
 * @param name     컬럼 이름. null이면 기본 이름(상태 코드 라벨)으로 되돌린다
 * @param wipLimit WIP 한도. null이면 없앤다
 */
public record UpdateBoardColumnCommand(String name, Integer wipLimit) {
}
