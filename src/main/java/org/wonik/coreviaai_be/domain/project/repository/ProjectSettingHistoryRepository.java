package org.wonik.coreviaai_be.domain.project.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.project.entity.ProjectSettingHistory;

public interface ProjectSettingHistoryRepository extends JpaRepository<ProjectSettingHistory, Long> {

	/** 설정 변경 이력(커서, id 내림차순 = 최신순). 호출자가 size + 1개를 요청한다. */
	@Query("""
		select h from ProjectSettingHistory h
		where h.projectId = :projectId
		  and (:cursorId is null or h.id < :cursorId)
		order by h.id desc
		""")
	List<ProjectSettingHistory> findPageByProjectId(
		@Param("projectId") Long projectId,
		@Param("cursorId") Long cursorId,
		Pageable pageable
	);
}
