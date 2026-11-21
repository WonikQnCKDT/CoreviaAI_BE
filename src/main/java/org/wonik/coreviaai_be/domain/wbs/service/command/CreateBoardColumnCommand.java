package org.wonik.coreviaai_be.domain.wbs.service.command;

import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;

/**
 * @param name           컬럼 이름(앞뒤 공백 제거). null이면 기본 이름(상태 코드 라벨)
 * @param statusCategory 이 컬럼으로 옮긴 카드의 상태
 * @param wipLimit       WIP 한도. null이면 없음
 */
public record CreateBoardColumnCommand(String name, WbsTaskStatus statusCategory, Integer wipLimit) {
}
