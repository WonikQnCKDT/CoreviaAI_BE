package org.wonik.coreviaai_be.domain.risk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 일정 지연 감지 설정(app.risk-analysis.*).
 * 임계값 UI(FR-ADM-03) 전까지 파일·환경변수로 두고, 실행 시 settings_snapshot에 남긴다.
 *
 * @param enabled                     스케줄러 동작 여부
 * @param cron                        매일 실행 cron(UTC). 기본 00:30 UTC ≈ 한국 09:30
 * @param progressBehindMarginPercent 진척률이 기간 경과율보다 이 값(퍼센트포인트) 이상 낮을 때만 PROGRESS_BEHIND
 * @param overloadThresholdPercent    할당 공수/가용 인일이 이 퍼센트를 초과할 때만 OVER_ALLOCATION (FR-AIR-03, 예: 120)
 */
@ConfigurationProperties(prefix = "app.risk-analysis")
public record RiskAnalysisProperties(
	boolean enabled,
	String cron,
	int progressBehindMarginPercent,
	int overloadThresholdPercent
) {
}
