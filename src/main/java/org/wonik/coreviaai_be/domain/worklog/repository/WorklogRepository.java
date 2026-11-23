package org.wonik.coreviaai_be.domain.worklog.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.worklog.entity.Worklog;

public interface WorklogRepository extends JpaRepository<Worklog, Long> {

	Optional<Worklog> findByUserIdAndLogDate(Long userId, LocalDate logDate);

	List<Worklog> findByUserIdAndLogDateBetweenOrderByLogDateAsc(Long userId, LocalDate from, LocalDate to);
}
