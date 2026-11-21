package org.wonik.coreviaai_be.domain.wbs.entity.type;

/**
 * 선후행 관계 유형(FR-WBS-03). MVP는 완료→시작만 쓰고, 다른 유형·지연 간격은 후속이다.
 */
public enum DependencyType {
	FINISH_TO_START
}
