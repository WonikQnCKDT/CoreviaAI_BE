package org.wonik.coreviaai_be.domain.risk.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 리스크 일일 스케줄 활성화.
 * error-log retention이 꺼져도(@ConditionalOnProperty) 이 모듈 스케줄은 동작해야 해서 따로 둔다.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.risk-analysis.enabled", havingValue = "true", matchIfMissing = true)
public class RiskAnalysisSchedulingConfig {
}
