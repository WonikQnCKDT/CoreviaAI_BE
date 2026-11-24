package org.wonik.coreviaai_be.domain.risk.entity.type;

/**
 * 리스크 감지 종류(risk_detections.detection_type).
 * FR-AIR-01은 DELAY만 쓰고, BOTTLENECK·OVERLOAD·MISSING은 후속(FR-AIR-02·03·누락)이다.
 */
public enum RiskDetectionType {
	DELAY,
	BOTTLENECK,
	OVERLOAD,
	MISSING
}
