package org.wonik.coreviaai_be.domain.wbs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsDependency;

public interface WbsDependencyRepository extends JpaRepository<WbsDependency, Long> {
}
