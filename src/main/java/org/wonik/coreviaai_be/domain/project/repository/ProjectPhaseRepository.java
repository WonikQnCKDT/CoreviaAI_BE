package org.wonik.coreviaai_be.domain.project.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.project.entity.ProjectPhase;

public interface ProjectPhaseRepository extends JpaRepository<ProjectPhase, Long> {

	List<ProjectPhase> findAllByProjectIdOrderByPhaseOrder(Long projectId);
}
