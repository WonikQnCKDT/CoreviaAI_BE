package org.wonik.coreviaai_be.domain.project.dto.response;

import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.service.result.ProjectMemberResult;

/**
 * 프로젝트 멤버 한 명과 역할(멤버 목록·초대·역할 변경 응답 항목).
 *
 * @param assignedTaskCount 담당 업무 수. 업무(FR-TSK-01) 구현 전이라 항상 0이다
 */
public record ProjectMemberResponse(Long userId, String name, ProjectActor actor, long assignedTaskCount) {

	public static ProjectMemberResponse from(final ProjectMemberResult result) {
		final ProjectMember member = result.member();
		return new ProjectMemberResponse(member.getUserId(), result.name(), member.getActor(), result.assignedTaskCount());
	}
}
