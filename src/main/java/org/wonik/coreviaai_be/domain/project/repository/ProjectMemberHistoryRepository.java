package org.wonik.coreviaai_be.domain.project.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMemberHistory;

public interface ProjectMemberHistoryRepository extends JpaRepository<ProjectMemberHistory, Long> {

	/** 프로젝트 멤버 이력(커서 방식, id 내림차순 = 최신순). 호출자가 size + 1개를 요청한다. */
	@Query("""
		select h from ProjectMemberHistory h
		where h.projectId = :projectId
		  and (:cursorId is null or h.id < :cursorId)
		order by h.id desc
		""")
	List<ProjectMemberHistory> findPageByProjectId(
		@Param("projectId") Long projectId,
		@Param("cursorId") Long cursorId,
		Pageable pageable
	);
}
