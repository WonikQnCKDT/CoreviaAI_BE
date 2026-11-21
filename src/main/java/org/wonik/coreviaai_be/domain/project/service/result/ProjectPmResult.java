package org.wonik.coreviaai_be.domain.project.service.result;

import java.util.List;

import org.wonik.coreviaai_be.domain.project.entity.Project;

/**
 * 관리자 PM 재지정 결과. 시스템 관리자가 호출하므로 프로젝트 상세 대신 프로젝트 식별 정보와 바뀐 PM 목록만 돌려준다.
 *
 * @param pms 재지정 후 프로젝트의 PM(등록순)
 */
public record ProjectPmResult(Project project, List<NamedRef> pms) {
}
