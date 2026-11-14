package org.wonik.coreviaai_be.domain.project.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectStatus;

public interface ProjectRepository extends JpaRepository<Project, Long> {

	boolean existsByCode(String code);

	/**
	 * 일반 사용자의 프로젝트 목록(커서 방식, id 내림차순 = 최신 생성순). null인 필터는 적용하지 않는다.
	 * 공개 범위(3.2 공통 규칙): 전사 공개, 주관 부서가 내 부서인 부서 공개, 내가 멤버인 프로젝트만 보인다.
	 * 다른 정렬 기준은 커서 구성이 복잡해져 후속 이슈에서 다룬다. 호출자가 size + 1개를 요청한다.
	 */
	@Query("""
		select p from Project p
		where (:cursorId is null or p.id < :cursorId)
		  and (:status is null or p.status = :status)
		  and (:departmentId is null or p.departmentId = :departmentId)
		  and (:pmUserId is null or p.pmUserId = :pmUserId)
		  and (p.visibility = org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility.COMPANY
		       or (p.visibility = org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility.DEPARTMENT
		           and :viewerDepartmentId is not null and p.departmentId = :viewerDepartmentId)
		       or exists (select m.id from ProjectMember m where m.projectId = p.id and m.userId = :viewerId))
		order by p.id desc
		""")
	List<Project> findPageForUser(
		@Param("cursorId") Long cursorId,
		@Param("status") ProjectStatus status,
		@Param("departmentId") Long departmentId,
		@Param("pmUserId") Long pmUserId,
		@Param("viewerId") Long viewerId,
		@Param("viewerDepartmentId") Long viewerDepartmentId,
		Pageable pageable
	);

	/**
	 * 경영진의 프로젝트 목록(커서 방식, id 내림차순). 소속 범위(본인 부서와 모든 하위 부서)의 프로젝트는
	 * 멤버 전용을 포함해 전부 보이고(3.1), 그 밖은 전사 공개만 보인다. scopeDepartmentIds는 비어 있지 않아야 한다.
	 */
	@Query("""
		select p from Project p
		where (:cursorId is null or p.id < :cursorId)
		  and (:status is null or p.status = :status)
		  and (:departmentId is null or p.departmentId = :departmentId)
		  and (:pmUserId is null or p.pmUserId = :pmUserId)
		  and (p.departmentId in :scopeDepartmentIds
		       or p.visibility = org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility.COMPANY)
		order by p.id desc
		""")
	List<Project> findPageForExecutive(
		@Param("cursorId") Long cursorId,
		@Param("status") ProjectStatus status,
		@Param("departmentId") Long departmentId,
		@Param("pmUserId") Long pmUserId,
		@Param("scopeDepartmentIds") Collection<Long> scopeDepartmentIds,
		Pageable pageable
	);
}
