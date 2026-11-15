package org.wonik.coreviaai_be.domain.project.dto.response;

import java.util.Map;

import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;

/**
 * 프로젝트 멤버 한 명과 역할.
 */
public record ProjectMemberResponse(Long userId, String name, ProjectActor actor) {

	public static ProjectMemberResponse of(final ProjectMember member, final Map<Long, String> userNames) {
		return new ProjectMemberResponse(member.getUserId(), userNames.get(member.getUserId()), member.getActor());
	}
}
