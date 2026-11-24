package org.wonik.coreviaai_be.domain.risk.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.wonik.coreviaai_be.domain.risk.service.RiskAnalysisRunner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** FR-AIR-01 매일 자동 계산. cron·enabled는 app.risk-analysis.* 설정. */
@Component
@ConditionalOnProperty(name = "app.risk-analysis.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class DelayRiskAnalysisScheduler {

	private final RiskAnalysisRunner runner;

	@Scheduled(cron = "${app.risk-analysis.cron:0 30 0 * * *}", zone = "UTC")
	public void runDailyDelayDetection() {
		try {
			runner.runDaily();
		} catch (final RuntimeException ex) {
			log.error("Scheduled risk analysis aborted", ex);
		}
	}
}
