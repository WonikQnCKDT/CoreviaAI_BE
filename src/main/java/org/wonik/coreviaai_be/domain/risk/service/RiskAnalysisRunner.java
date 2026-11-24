package org.wonik.coreviaai_be.domain.risk.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.ProjectFeatureAssignment;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectStatus;
import org.wonik.coreviaai_be.domain.project.repository.ProjectFeatureAssignmentRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectRepository;
import org.wonik.coreviaai_be.domain.risk.config.RiskAnalysisProperties;
import org.wonik.coreviaai_be.domain.risk.entity.RiskAnalysisRun;
import org.wonik.coreviaai_be.domain.risk.entity.RiskDetection;
import org.wonik.coreviaai_be.domain.risk.repository.RiskAnalysisRunRepository;
import org.wonik.coreviaai_be.domain.risk.repository.RiskDetectionRepository;
import org.wonik.coreviaai_be.domain.risk.service.result.DetectedRisk;
import org.wonik.global.common.time.BusinessCalendar;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 일일 리스크 계산 오케스트레이션(UC-AIR-01의 일정 지연 부분, FR-AIR-01).
 * 성공한 당일 실행은 건너뛰고, FAILED·RUNNING(중단)은 같은 run_date 행을 재사용한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAnalysisRunner {

	private static final Set<ProjectStatus> EXCLUDED_STATUSES =
		EnumSet.of(ProjectStatus.COMPLETED, ProjectStatus.CANCELLED);

	private final RiskAnalysisRunRepository runRepository;
	private final RiskDetectionRepository detectionRepository;
	private final ProjectFeatureAssignmentRepository featureAssignmentRepository;
	private final ProjectRepository projectRepository;
	private final DelayDetectionService delayDetectionService;
	private final RiskAnalysisProperties properties;
	private final Clock clock;

	/**
	 * @return 저장된 감지 건수. 이미 성공한 당일 실행이면 -1, 실패로 FAILED만 남기면 -2
	 */
	@Transactional
	public int runDaily() {
		final LocalDate runDate = BusinessCalendar.today(clock);
		final Instant startedAt = clock.instant();
		final String settingsSnapshot = settingsSnapshotJson();

		final Optional<RiskAnalysisRun> existing = runRepository.findByRunDate(runDate);
		if (existing.isPresent() && existing.get().isSucceeded()) {
			log.info("Risk analysis already succeeded for runDate={}", runDate);
			return -1;
		}

		final RiskAnalysisRun run = existing.map(row -> {
			detectionRepository.deleteAllByRunId(row.getId());
			row.restart(startedAt, settingsSnapshot);
			return row;
		}).orElseGet(() -> runRepository.save(RiskAnalysisRun.start(runDate, startedAt, settingsSnapshot)));

		try {
			final List<Long> projectIds = targetProjectIds();
			final List<RiskDetection> toSave = new ArrayList<>();
			for (final Long projectId : projectIds) {
				final List<DetectedRisk> detected = delayDetectionService.detectForProject(projectId, runDate);
				for (final DetectedRisk item : detected) {
					toSave.add(RiskDetection.delay(
						run.getId(),
						item.projectId(),
						item.reasonCode(),
						item.targetType(),
						item.targetId(),
						item.metricValue(),
						item.detailsJson()));
				}
			}
			if (!toSave.isEmpty()) {
				detectionRepository.saveAll(toSave);
			}
			run.succeed(clock.instant());
			log.info("Risk analysis succeeded: runDate={}, projects={}, detections={}",
				runDate, projectIds.size(), toSave.size());
			return toSave.size();
		} catch (final RuntimeException ex) {
			// 예외를 다시 던지면 트랜잭션 롤백으로 FAILED 상태도 사라지므로, 기록만 남기고 정상 종료한다.
			run.fail(clock.instant());
			detectionRepository.deleteAllByRunId(run.getId());
			log.error("Risk analysis failed: runDate={}", runDate, ex);
			return -2;
		}
	}

	private List<Long> targetProjectIds() {
		final List<Long> wbsProjectIds = featureAssignmentRepository.findAllByFeature(ProjectFeature.WBS).stream()
			.map(ProjectFeatureAssignment::getProjectId)
			.distinct()
			.toList();
		if (wbsProjectIds.isEmpty()) {
			return List.of();
		}
		return projectRepository.findAllById(wbsProjectIds).stream()
			.filter(project -> !EXCLUDED_STATUSES.contains(project.getStatus()))
			.map(Project::getId)
			.sorted()
			.toList();
	}

	private String settingsSnapshotJson() {
		return "{\"progressBehindMarginPercent\":" + properties.progressBehindMarginPercent()
			+ ",\"cron\":\"" + escapeJson(properties.cron()) + "\"}";
	}

	private static String escapeJson(final String value) {
		if (value == null) {
			return "";
		}
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
