package org.wonik.coreviaai_be.domain.project.service.result;

import java.util.List;
import java.util.Map;

import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;

/**
 * 프로젝트 상세: 운영 방식과 관계없는 공통 정보(기본 정보·운영 방식·지정 액터·멤버).
 * 운영 방식별 구조(폭포수 단계·게이트, 이후 애자일 스프린트 등)는 각자의 조회로 분리한다
 * (예: {@link ProjectPhasesResult}). 운영 방식이 늘어도 상세 계약이 바뀌지 않게 하기 위해서다.
 *
 * @param userNames 멤버·PM 이름 표시용 사용자 이름(userId → name)
 */
public record ProjectDetailResult(
	Project project,
	String departmentName,
	List<ProjectActor> actors,
	List<ProjectMember> members,
	Map<Long, String> userNames
) {
}
