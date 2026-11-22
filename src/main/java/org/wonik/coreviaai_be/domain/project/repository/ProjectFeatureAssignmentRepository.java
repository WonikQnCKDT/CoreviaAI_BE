package org.wonik.coreviaai_be.domain.project.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.project.entity.ProjectFeatureAssignment;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;

public interface ProjectFeatureAssignmentRepository extends JpaRepository<ProjectFeatureAssignment, Long> {

	List<ProjectFeatureAssignment> findAllByProjectIdOrderById(Long projectId);

	/** 기능 모듈 사용 여부(FR-PRJ-06). 사용하지 않는 모듈의 API는 막는다. */
	boolean existsByProjectIdAndFeature(Long projectId, ProjectFeature feature);

	/** 주어진 기능 모듈 중 하나라도 켜져 있는지. 여러 모듈이 함께 쓰는 API(업무 상세 등)에 쓴다. */
	boolean existsByProjectIdAndFeatureIn(Long projectId, Collection<ProjectFeature> features);
}
