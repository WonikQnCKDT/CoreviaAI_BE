package org.wonik.coreviaai_be.domain.project.dto.response;

import java.util.Arrays;
import java.util.List;

import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;

/**
 * ③ 부서 유형별 액터 목록. 공통 액터(PM)는 모든 부서 유형에 포함된다.
 */
public record DepartmentTypeOptionResponse(DepartmentType value, boolean enabled, List<ActorOptionResponse> actors) {

	public static DepartmentTypeOptionResponse from(final DepartmentType departmentType) {
		final List<ActorOptionResponse> actors = Arrays.stream(ProjectActor.values())
			.filter(actor -> actor.isCommon() || actor.getDepartmentType() == departmentType)
			.map(actor -> ActorOptionResponse.of(actor, departmentType))
			.toList();
		return new DepartmentTypeOptionResponse(departmentType, departmentType.isEnabled(), actors);
	}
}
