package org.wonik.coreviaai_be.domain.project.service.result;

import java.util.List;
import java.util.Map;

import org.wonik.coreviaai_be.domain.project.entity.ProjectPhase;

/**
 * 프로젝트의 사용자 정의 단계·게이트(FR-PRJ-01 ②, FR-PRJ-05). 단계 순서대로 정렬돼 있다.
 *
 * @param userNames 게이트 승인자 이름 표시용(userId → name)
 */
public record ProjectPhasesResult(List<ProjectPhase> phases, Map<Long, String> userNames,
	Map<Long, Integer> progressByPhaseId) {
}
