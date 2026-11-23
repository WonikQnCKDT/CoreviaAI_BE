package org.wonik.coreviaai_be.domain.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.project.entity.ProjectPhase;

public interface ProjectPhaseRepository extends JpaRepository<ProjectPhase, Long> {

	List<ProjectPhase> findAllByProjectIdOrderByPhaseOrder(Long projectId);

	Optional<ProjectPhase> findByIdAndProjectId(Long id, Long projectId);

	long countByProjectId(Long projectId);

	/** 이 사용자가 한 단계 이상의 게이트 승인자인지(FR-PRJ-04: 승인자는 멤버 제외·PM 지정 불가). */
	boolean existsByProjectIdAndGateApproverUserId(Long projectId, Long gateApproverUserId);
}
