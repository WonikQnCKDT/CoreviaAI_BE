package org.wonik.coreviaai_be.domain.wbs.service.command;

import java.util.List;

/** 보드의 모든 컬럼 ID를 새 표시 순서대로. */
public record ReorderBoardColumnsCommand(List<Long> columnIds) {
}
