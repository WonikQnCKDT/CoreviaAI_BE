package org.wonik.coreviaai_be.domain.department.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.coreviaai_be.domain.department.entity.DepartmentStatus;
import org.wonik.coreviaai_be.domain.department.repository.DepartmentRepository;

import lombok.RequiredArgsConstructor;

/**
 * 일반 사용자용 부서 목록(FR-PRJ-01 ① 주관 부서 선택). 지정할 수 있는 활성 부서만 준다.
 * 관리용 목록·상태 변경은 AdminDepartmentService가 맡는다.
 */
@Service
@RequiredArgsConstructor
public class DepartmentQueryService {

	/** 한 번에 돌려주는 최대 부서 수. 전사 부서 수보다 넉넉한 상한이며 넘으면 이름순 앞의 1000개만 준다. */
	public static final int MAX_DEPARTMENTS = 1000;

	private final DepartmentRepository departmentRepository;

	/** 활성 부서를 이름 오름차순(같은 이름은 ID 오름차순)으로 최대 MAX_DEPARTMENTS개 준다. */
	@Transactional(readOnly = true)
	public List<Department> findActive() {
		return departmentRepository.findByStatusOrderByNameAscIdAsc(
			DepartmentStatus.ACTIVE,
			PageRequest.of(0, MAX_DEPARTMENTS)
		);
	}
}
