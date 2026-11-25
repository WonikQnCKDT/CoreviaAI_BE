package org.wonik.coreviaai_be.domain.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.ProjectFeatureAssignment;
import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectTemplate;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility;
import org.wonik.coreviaai_be.domain.project.repository.ProjectFeatureAssignmentRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectRepository;
import org.wonik.coreviaai_be.domain.risk.entity.RiskDetection;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskDetectionType;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskReasonCode;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskRunStatus;
import org.wonik.coreviaai_be.domain.risk.repository.RiskAnalysisRunRepository;
import org.wonik.coreviaai_be.domain.risk.repository.RiskDetectionRepository;
import org.wonik.coreviaai_be.domain.risk.service.RiskAnalysisRunner;
import org.wonik.coreviaai_be.domain.wbs.entity.Milestone;
import org.wonik.coreviaai_be.domain.wbs.entity.MilestoneTask;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskDetails;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsDependency;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.coreviaai_be.domain.wbs.repository.MilestoneRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.MilestoneTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsDependencyRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.support.ApiIntegrationTestSupport;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RiskAnalysisIntegrationTest extends ApiIntegrationTestSupport {

	/** 한국 날짜 2026-11-24. */
	private static final Instant AS_OF = Instant.parse("2026-11-23T15:00:00Z");
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 24);

	@Autowired
	private RiskAnalysisRunner runner;
	@Autowired
	private RiskAnalysisRunRepository runRepository;
	@Autowired
	private RiskDetectionRepository detectionRepository;
	@Autowired
	private ProjectRepository projectRepository;
	@Autowired
	private ProjectFeatureAssignmentRepository featureAssignmentRepository;
	@Autowired
	private WbsTaskRepository taskRepository;
	@Autowired
	private WbsDependencyRepository dependencyRepository;
	@Autowired
	private MilestoneRepository milestoneRepository;
	@Autowired
	private MilestoneTaskRepository milestoneTaskRepository;

	@Test
	@DisplayName("마감 경과·진척 지연·선행 전파·마일스톤 위험을 한 실행에 저장하고 재실행은 건너뛴다")
	void detectsFourDelayReasonsAndSkipsSucceededRerun() {
		clock.setInstant(AS_OF);
		final User pm = saveUser("risk-pm", USER_PASSWORD, SystemRole.USER, null);
		final Project project = projectRepository.save(Project.create(
			"RISK01", "리스크 프로젝트", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31),
			ProjectVisibility.COMPANY, ProjectTemplate.IT_PROJECT, Methodology.WATERFALL,
			DepartmentType.SW, pm.getId()));
		featureAssignmentRepository.save(ProjectFeatureAssignment.of(project.getId(), ProjectFeature.WBS));

		final WbsTask overdue = taskRepository.save(WbsTask.task(
			project.getId(), 0,
			new TaskDetails("마감경과", null, null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 20), null),
			WbsItemSource.MANUAL, pm.getId()));
		overdue.changeProgress(10);
		taskRepository.save(overdue);

		final WbsTask behind = taskRepository.save(WbsTask.task(
			project.getId(), 1,
			new TaskDetails("진척지연", null, null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1), null),
			WbsItemSource.MANUAL, pm.getId()));
		behind.changeProgress(10);
		taskRepository.save(behind);

		final WbsTask predecessor = taskRepository.save(WbsTask.task(
			project.getId(), 2,
			new TaskDetails("선행", null, null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 20), null),
			WbsItemSource.MANUAL, pm.getId()));
		final WbsTask successor = taskRepository.save(WbsTask.task(
			project.getId(), 3,
			new TaskDetails("후행", null, null, LocalDate.of(2026, 11, 21), LocalDate.of(2026, 12, 1), null),
			WbsItemSource.MANUAL, pm.getId()));
		dependencyRepository.save(WbsDependency.finishToStart(
			project.getId(), predecessor.getId(), successor.getId(), pm.getId()));

		final WbsTask linked = taskRepository.save(WbsTask.task(
			project.getId(), 4,
			new TaskDetails("마일스톤연결", null, null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 10), null),
			WbsItemSource.MANUAL, pm.getId()));
		final Milestone milestone = milestoneRepository.save(Milestone.of(
			project.getId(), "MS", LocalDate.of(2026, 12, 1), WbsItemSource.MANUAL, pm.getId()));
		milestoneTaskRepository.save(MilestoneTask.of(milestone.getId(), linked.getId()));

		final int saved = runner.runDaily();
		assertThat(saved).isGreaterThanOrEqualTo(4);

		final var run = runRepository.findByRunDate(TODAY).orElseThrow();
		assertThat(run.getStatus()).isEqualTo(RiskRunStatus.SUCCEEDED);
		assertThat(run.getSettingsSnapshot()).contains("progressBehindMarginPercent", "overloadThresholdPercent");

		final List<RiskDetection> detections = detectionRepository.findAllByRunIdOrderByIdAsc(run.getId());
		assertThat(detections).extracting(RiskDetection::getDetectionType).doesNotContain(RiskDetectionType.OVERLOAD);
		assertThat(detections).extracting(RiskDetection::getReasonCode)
			.contains(
				RiskReasonCode.OVERDUE,
				RiskReasonCode.PROGRESS_BEHIND,
				RiskReasonCode.PREDECESSOR_DELAY,
				RiskReasonCode.MILESTONE_AT_RISK);
		assertThat(detections.stream()
			.filter(d -> d.getReasonCode() == RiskReasonCode.OVERDUE)
			.findFirst().orElseThrow().getMetricValue())
			.isEqualByComparingTo(BigDecimal.valueOf(4));

		assertThat(runner.runDaily()).isEqualTo(-1);
		assertThat(detectionRepository.findAllByRunIdOrderByIdAsc(run.getId())).hasSize(detections.size());
	}
}
