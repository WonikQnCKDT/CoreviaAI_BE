package org.wonik.coreviaai_be.domain.project.dto.response;

import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.service.result.ProjectMemberResult;

/**
 * 프로젝트 멤버 한 명과 역할(GET /api/projects/{id}/members의 항목).
 */
public record ProjectMemberResponse(Long userId, String name, ProjectActor actor) {

	public static ProjectMemberResponse from(final ProjectMemberResult result) {
		final ProjectMember member = result.member();
		return new ProjectMemberResponse(member.getUserId(), result.name(), member.getActor());
	}
}
