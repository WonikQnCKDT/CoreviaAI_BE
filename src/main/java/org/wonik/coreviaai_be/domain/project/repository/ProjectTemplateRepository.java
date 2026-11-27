package org.wonik.coreviaai_be.domain.project.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.project.entity.ProjectTemplateDefinition;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectTemplate;

import jakarta.persistence.LockModeType;

public interface ProjectTemplateRepository extends JpaRepository<ProjectTemplateDefinition, Long> {

	List<ProjectTemplateDefinition> findAllByStatusOrderByIdAsc(String status);

	Optional<ProjectTemplateDefinition> findByTemplateKey(ProjectTemplate templateKey);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select t from ProjectTemplateDefinition t where t.templateKey = :templateKey")
	Optional<ProjectTemplateDefinition> findByTemplateKeyForUpdate(@Param("templateKey") ProjectTemplate templateKey);
}
