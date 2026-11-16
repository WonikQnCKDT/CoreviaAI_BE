package org.wonik.coreviaai_be.domain.project.dto.request;

import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;

import jakarta.validation.constraints.NotNull;

/**
 * 멤버 역할 변경(FR-PRJ-04). actor는 프로젝트에 지정한 액터 중 PM이 아닌 값이어야 한다(서비스에서 검증).
 */
public record ChangeMemberRoleRequest(
	@NotNull(message = "변경할 역할을 선택해 주세요")
	ProjectActor actor
) {
}
