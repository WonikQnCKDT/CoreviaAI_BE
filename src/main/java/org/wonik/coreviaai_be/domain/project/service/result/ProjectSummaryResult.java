package org.wonik.coreviaai_be.domain.project.service.result;

import org.wonik.coreviaai_be.domain.project.entity.Project;

/**
 * 프로젝트 목록 한 항목. 이름은 목록 화면 표시용으로 서비스가 채운다(없는 사용자·부서면 null).
 */
public record ProjectSummaryResult(Project project, String departmentName, String pmName) {
}
