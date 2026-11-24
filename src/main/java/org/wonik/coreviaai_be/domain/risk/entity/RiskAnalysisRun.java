package org.wonik.coreviaai_be.domain.risk.entity;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskRunStatus;

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
 * 리스크 일일 계산 실행 한 건(risk_analysis_runs, FR-AIR-01~04·06).
 * 하루(run_date)당 한 행이며, 그날 적용한 임계값·가중치는 settings_snapshot에 남긴다.
 */
@Getter
@Entity
@Table(name = "risk_analysis_runs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RiskAnalysisRun {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "run_date", nullable = false, unique = true)
	private LocalDate runDate;

	@Enumerated(EnumType.STRING)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	@Column(name = "status", nullable = false, length = 20)
	private RiskRunStatus status;

	@Column(name = "started_at", nullable = false)
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "settings_snapshot", nullable = false)
	private String settingsSnapshot;

	private RiskAnalysisRun(
		final LocalDate runDate,
		final Instant startedAt,
		final String settingsSnapshot
	) {
		this.runDate = runDate;
		this.status = RiskRunStatus.RUNNING;
		this.startedAt = startedAt;
		this.settingsSnapshot = settingsSnapshot;
	}

	public static RiskAnalysisRun start(
		final LocalDate runDate,
		final Instant startedAt,
		final String settingsSnapshot
	) {
		return new RiskAnalysisRun(runDate, startedAt, settingsSnapshot);
	}

	/** 이미 끝난 실행을 같은 run_date로 다시 돌릴 때 상태를 초기화한다(UK 유지). */
	public void restart(final Instant startedAt, final String settingsSnapshot) {
		this.status = RiskRunStatus.RUNNING;
		this.startedAt = startedAt;
		this.finishedAt = null;
		this.settingsSnapshot = settingsSnapshot;
	}

	public void succeed(final Instant finishedAt) {
		this.status = RiskRunStatus.SUCCEEDED;
		this.finishedAt = finishedAt;
	}

	public void fail(final Instant finishedAt) {
		this.status = RiskRunStatus.FAILED;
		this.finishedAt = finishedAt;
	}

	public boolean isSucceeded() {
		return status == RiskRunStatus.SUCCEEDED;
	}
}
