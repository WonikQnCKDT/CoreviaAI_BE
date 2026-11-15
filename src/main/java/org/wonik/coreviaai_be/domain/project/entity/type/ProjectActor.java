package org.wonik.coreviaai_be.domain.project.entity.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 역할(액터, 요구사항 명세서 3.2). PM은 부서 유형과 무관한 공통 필수 액터라 departmentType이 null이다.
 * 액터별 선택 가능 여부는 소속 부서 유형의 enabled를 따르므로, 부서 유형을 활성화하면 소속 액터가 함께 열린다.
 */
@Getter
@RequiredArgsConstructor
public enum ProjectActor {
	PM(null),
	BUSINESS_OWNER(DepartmentType.SW),
	DEVELOPER(DepartmentType.SW),
	QA(DepartmentType.SW),
	DEPT_STAFF(DepartmentType.MANUFACTURING),
	CUSTOMER_LIAISON(DepartmentType.MANUFACTURING);

	private final DepartmentType departmentType;

	/** PM은 모든 부서 유형에 공통이다. */
	public boolean isCommon() {
		return departmentType == null;
	}

	/** 이 액터를 해당 부서 유형의 프로젝트에서 지정할 수 있는지. 공통 액터는 부서 유형이 활성일 때 함께 선택 가능하다. */
	public boolean selectableFor(final DepartmentType type) {
		return type.isEnabled() && (isCommon() || departmentType == type);
	}
}
