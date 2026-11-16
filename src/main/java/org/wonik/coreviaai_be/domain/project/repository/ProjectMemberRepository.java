package org.wonik.coreviaai_be.domain.project.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

	boolean existsByProjectIdAndUserId(Long projectId, Long userId);

	Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

	List<ProjectMember> findAllByProjectIdAndUserIdIn(Long projectId, Collection<Long> userIds);

	/**
	 * 프로젝트 멤버 목록(커서 방식, id 오름차순 = 등록순). 생성자 PM이 첫 행으로 저장되므로 생성 직후에는 PM이 맨 앞이다.
	 * 호출자가 size + 1개를 요청한다.
	 */
	@Query("""
		select m from ProjectMember m
		where m.projectId = :projectId
		  and (:cursorId is null or m.id > :cursorId)
		order by m.id asc
		""")
	List<ProjectMember> findPageByProjectId(
		@Param("projectId") Long projectId,
		@Param("cursorId") Long cursorId,
		Pageable pageable
	);
}
