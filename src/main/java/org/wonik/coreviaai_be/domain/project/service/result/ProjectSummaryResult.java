package org.wonik.coreviaai_be.domain.project.service.result;

import java.util.List;

import org.wonik.coreviaai_be.domain.project.entity.Project;

/**
 * 프로젝트 목록 한 항목. 주관 부서·PM 이름은 목록 화면 표시용으로 서비스가 채운다.
 *
 * @param departments 주관 부서(0~N개, ID 순)
 * @param pms         PM(1명 이상, 등록순)
 */
public record ProjectSummaryResult(Project project, List<NamedRef> departments, List<NamedRef> pms) {
}
