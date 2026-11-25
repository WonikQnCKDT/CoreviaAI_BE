package org.wonik.coreviaai_be.domain.risk.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.wonik.coreviaai_be.domain.risk.entity.RiskAnalysisRun;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskRunStatus;

public interface RiskAnalysisRunRepository extends JpaRepository<RiskAnalysisRun, Long> {

	Optional<RiskAnalysisRun> findByRunDate(LocalDate runDate);

	/** 조회 화면은 가장 최근 성공 실행의 감지 결과를 보여 준다. */
	Optional<RiskAnalysisRun> findFirstByStatusOrderByRunDateDesc(RiskRunStatus status);
}
