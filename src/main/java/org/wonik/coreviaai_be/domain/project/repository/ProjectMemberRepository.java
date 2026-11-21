package org.wonik.coreviaai_be.domain.project.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

	boolean existsByProjectIdAndUserId(Long projectId, Long userId);

	Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

	List<ProjectMember> findAllByProjectIdAndUserIdIn(Long projectId, Collection<Long> userIds);

	boolean existsByProjectIdAndUserIdAndActor(Long projectId, Long userId, ProjectActor actor);

	/** PM 최소 1명 규칙(FR-PRJ-04) 판정용. */
	long countByProjectIdAndActor(Long projectId, ProjectActor actor);

	/** 프로젝트 전체 멤버(등록순). 멤버 수 상한이 없어 화면 목록은 커서 조회를 쓰고, 이 메서드는 서버 내부 판정·AI 입력에만 쓴다. */
	List<ProjectMember> findAllByProjectIdOrderById(Long projectId);

	List<ProjectMember> findAllByProjectIdAndActorOrderById(Long projectId, ProjectActor actor);

	/** 목록 화면에서 한 페이지 프로젝트의 PM을 한 번에 읽는다(N+1 방지). */
	List<ProjectMember> findAllByProjectIdInAndActor(Collection<Long> projectIds, ProjectActor actor);

	/**
	 * 프로젝트 멤버 목록(커서 방식, id 오름차순 = 등록순). 생성자(첫 PM)가 첫 행으로 저장되므로 생성 직후에는 생성자가 맨 앞이다.
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
