package org.wonik.coreviaai_be.domain.project.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

	List<ProjectMember> findAllByProjectIdOrderById(Long projectId);

	boolean existsByProjectIdAndUserId(Long projectId, Long userId);
}
