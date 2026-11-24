package org.wonik.coreviaai_be.domain.risk.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.risk.entity.RiskAnalysisRun;

public interface RiskAnalysisRunRepository extends JpaRepository<RiskAnalysisRun, Long> {

	Optional<RiskAnalysisRun> findByRunDate(LocalDate runDate);
}
