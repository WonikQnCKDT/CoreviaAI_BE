package org.wonik.coreviaai_be.domain.project.dto.response;

import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;

/**
 * ③ 액터 선택지. required가 true(PM)면 기본 선택 상태로 두고 해제를 막는다.
 */
public record ActorOptionResponse(ProjectActor value, boolean enabled, boolean required) {

	public static ActorOptionResponse of(final ProjectActor actor, final DepartmentType departmentType) {
		return new ActorOptionResponse(actor, actor.selectableFor(departmentType), actor == ProjectActor.PM);
	}
}
