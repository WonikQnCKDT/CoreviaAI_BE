package org.wonik.coreviaai_be.domain.project.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.project.entity.ProjectActorAssignment;

public interface ProjectActorAssignmentRepository extends JpaRepository<ProjectActorAssignment, Long> {

	List<ProjectActorAssignment> findAllByProjectIdOrderById(Long projectId);
}
