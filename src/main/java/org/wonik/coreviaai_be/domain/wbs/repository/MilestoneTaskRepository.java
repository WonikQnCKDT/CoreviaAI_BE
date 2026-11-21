package org.wonik.coreviaai_be.domain.wbs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.wbs.entity.MilestoneTask;

public interface MilestoneTaskRepository extends JpaRepository<MilestoneTask, Long> {

	boolean existsByTaskId(Long taskId);
}
