package org.wonik.coreviaai_be.domain.risk.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 리스크 계산 설정 바인딩. 스케줄러가 꺼져도 Runner·규칙 테스트에서 Properties를 쓴다. */
@Configuration
@EnableConfigurationProperties(RiskAnalysisProperties.class)
public class RiskAnalysisConfig {
}
