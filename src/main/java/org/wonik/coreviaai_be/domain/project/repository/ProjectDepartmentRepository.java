package org.wonik.coreviaai_be.domain.project.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.project.entity.ProjectDepartment;

public interface ProjectDepartmentRepository extends JpaRepository<ProjectDepartment, Long> {

	List<ProjectDepartment> findAllByProjectIdOrderById(Long projectId);

	/** 목록 화면에서 한 페이지 프로젝트의 주관 부서를 한 번에 읽는다(N+1 방지). */
	List<ProjectDepartment> findAllByProjectIdIn(Collection<Long> projectIds);

	boolean existsByProjectIdAndDepartmentIdIn(Long projectId, Collection<Long> departmentIds);
}
