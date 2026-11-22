package org.wonik.coreviaai_be.domain.wbs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.wbs.entity.Milestone;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

	/** 프로젝트 마일스톤 전체를 목표일 순(같으면 ID 순)으로. 프로젝트당 개수 상한이 있어 페이지로 나누지 않는다. */
	List<Milestone> findAllByProjectIdOrderByTargetDateAscIdAsc(Long projectId);

	/** 다른 프로젝트의 마일스톤 ID로 접근하지 못하도록 항상 프로젝트와 함께 찾는다. */
	Optional<Milestone> findByIdAndProjectId(Long id, Long projectId);

	/** 프로젝트당 마일스톤 수 상한 판정용. */
	long countByProjectId(Long projectId);

	/**
	 * 연결 작업만 바꿨을 때 마일스톤 version을 올린다(엔티티 필드 변경이 없어 @Version이 스스로 오르지 않음).
	 * 연결 변경도 같은 version으로 보호해, 다른 PM이 그 사이 고친 마일스톤을 덮어쓰지 않게 한다.
	 * 영속성 컨텍스트를 비우므로 호출 뒤에는 마일스톤을 다시 읽는다.
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update Milestone m set m.version = m.version + 1 where m.id = :id")
	int incrementVersion(@Param("id") Long id);
}
