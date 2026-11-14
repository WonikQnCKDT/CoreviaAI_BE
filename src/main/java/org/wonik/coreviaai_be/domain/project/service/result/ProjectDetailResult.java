package org.wonik.coreviaai_be.domain.project.service.result;

import java.util.List;
import java.util.Map;

import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.ProjectPhase;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;

/**
 * 프로젝트 상세: 운영 방식·지정 액터·멤버·단계(FR-PRJ-01 인수 기준의 '운영 방식을 프로젝트 정보에 표시').
 *
 * @param userNames 멤버·PM 이름 표시용 사용자 이름(userId → name)
 */
public record ProjectDetailResult(
	Project project,
	String departmentName,
	List<ProjectActor> actors,
	List<ProjectMember> members,
	List<ProjectPhase> phases,
	Map<Long, String> userNames
) {
}
