package org.wonik.coreviaai_be.domain.risk.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.wonik.coreviaai_be.domain.risk.entity.RiskDetection;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskDetectionType;

public interface RiskDetectionRepository extends JpaRepository<RiskDetection, Long> {

	List<RiskDetection> findAllByRunIdOrderByIdAsc(Long runId);

	List<RiskDetection> findAllByRunIdAndProjectIdAndDetectionTypeOrderByTargetIdAsc(
		Long runId,
		Long projectId,
		RiskDetectionType detectionType
	);

	/** 같은 run_date를 재실행할 때 이전 감지 행을 지운다(실행 단위 교체). */
	@Modifying(flushAutomatically = true)
	@Query("delete from RiskDetection d where d.runId = :runId")
	int deleteAllByRunId(@Param("runId") Long runId);
}
