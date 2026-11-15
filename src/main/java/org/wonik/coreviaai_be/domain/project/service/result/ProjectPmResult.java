package org.wonik.coreviaai_be.domain.project.service.result;

import org.wonik.coreviaai_be.domain.project.entity.Project;

/**
 * PM 위임·재지정 결과. 위임 후 기존 PM이 멤버에서 빠지면 상세를 볼 수 없을 수 있고, 재지정은 시스템 관리자가 호출하므로
 * 프로젝트 상세 대신 바뀐 PM만 돌려준다.
 */
public record ProjectPmResult(Project project, String pmName) {
}
