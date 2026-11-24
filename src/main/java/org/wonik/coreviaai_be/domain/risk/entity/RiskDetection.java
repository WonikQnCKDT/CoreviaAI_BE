package org.wonik.coreviaai_be.domain.risk.entity;

import java.math.BigDecimal;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskDetectionType;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskReasonCode;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskTargetType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리스크 감지 결과 한 건(risk_detections). 추가만 하는 기록이라 수정 메서드가 없다.
 * impact_days는 FR-AIR-04 파급효과 몫이라 AIR-01에서는 null로 둔다.
 */
@Getter
@Entity
@Table(name = "risk_detections")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RiskDetection {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "run_id", nullable = false, updatable = false)
	private Long runId;

	@Column(name = "project_id", nullable = false, updatable = false)
	private Long projectId;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "detection_type", nullable = false, updatable = false, length = 20)
	private RiskDetectionType detectionType;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "reason_code", nullable = false, updatable = false, length = 50)
	private RiskReasonCode reasonCode;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "target_type", nullable = false, updatable = false, length = 30)
	private RiskTargetType targetType;

	@Column(name = "target_id", nullable = false, updatable = false)
	private Long targetId;

	@Column(name = "metric_value", precision = 10, scale = 2, updatable = false)
	private BigDecimal metricValue;

	@Column(name = "impact_days", updatable = false)
	private Integer impactDays;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "details", updatable = false)
	private String details;

	private RiskDetection(
		final Long runId,
		final Long projectId,
		final RiskDetectionType detectionType,
		final RiskReasonCode reasonCode,
		final RiskTargetType targetType,
		final Long targetId,
		final BigDecimal metricValue,
		final String details
	) {
		this.runId = runId;
		this.projectId = projectId;
		this.detectionType = detectionType;
		this.reasonCode = reasonCode;
		this.targetType = targetType;
		this.targetId = targetId;
		this.metricValue = metricValue;
		this.details = details;
	}

	public static RiskDetection delay(
		final Long runId,
		final Long projectId,
		final RiskReasonCode reasonCode,
		final RiskTargetType targetType,
		final Long targetId,
		final BigDecimal metricValue,
		final String details
	) {
		return new RiskDetection(
			runId, projectId, RiskDetectionType.DELAY, reasonCode, targetType, targetId, metricValue, details);
	}
}
