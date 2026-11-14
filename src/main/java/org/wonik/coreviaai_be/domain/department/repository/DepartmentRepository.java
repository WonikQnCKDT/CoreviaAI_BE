package org.wonik.coreviaai_be.domain.department.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.coreviaai_be.domain.department.entity.DepartmentStatus;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

	/**
	 * 관리자 부서 목록(커서 방식, id 오름차순). 상위 부서가 하위보다 먼저 만들어지므로 오름차순이면
	 * FE가 페이지를 이어 받으며 트리를 구성하기 쉽다. 호출자가 size + 1개를 요청한다.
	 */
	@Query("""
		select d from Department d
		where (:cursorId is null or d.id > :cursorId)
		  and (:status is null or d.status = :status)
		order by d.id asc
		""")
	List<Department> findPageForAdmin(
		@Param("cursorId") Long cursorId,
		@Param("status") DepartmentStatus status,
		Pageable pageable
	);

	boolean existsByParentIdAndName(Long parentId, String name);

	/** UNIQUE(parent_id, name)은 NULL끼리 다르다고 보므로 최상위 부서끼리의 이름 중복은 DB가 막지 못한다. */
	boolean existsByParentIdIsNullAndName(String name);

	boolean existsByParentIdAndStatus(Long parentId, DepartmentStatus status);
}
