package org.wonik.coreviaai_be.domain.risk.service.result;

import java.math.BigDecimal;

import org.wonik.coreviaai_be.domain.risk.entity.type.RiskReasonCode;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskTargetType;

/**
 * 저장 직전 지연 감지 후보(FR-AIR-01).
 *
 * @param metricValue 판정 수치(지연일·경과율 차이 등)
 * @param detailsJson 부가 JSON(선행 작업 ID 등). 없으면 null
 */
public record DetectedRisk(
	Long projectId,
	RiskReasonCode reasonCode,
	RiskTargetType targetType,
	Long targetId,
	BigDecimal metricValue,
	String detailsJson
) {
}
