package org.wonik.coreviaai_be.domain.wbs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.wbs.entity.Milestone;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {
}
