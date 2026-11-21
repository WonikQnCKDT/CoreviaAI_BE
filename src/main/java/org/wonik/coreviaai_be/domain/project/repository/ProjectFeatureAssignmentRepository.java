package org.wonik.coreviaai_be.domain.project.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.project.entity.ProjectFeatureAssignment;

public interface ProjectFeatureAssignmentRepository extends JpaRepository<ProjectFeatureAssignment, Long> {

	List<ProjectFeatureAssignment> findAllByProjectIdOrderById(Long projectId);
}
