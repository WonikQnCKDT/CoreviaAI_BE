package org.wonik.coreviaai_be.domain.risk.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.wonik.coreviaai_be.domain.risk.config.RiskAnalysisProperties;
import org.wonik.coreviaai_be.domain.risk.policy.MilestoneAtRiskRule;
import org.wonik.coreviaai_be.domain.risk.policy.OverdueRule;
import org.wonik.coreviaai_be.domain.risk.policy.PredecessorDelayRule;
import org.wonik.coreviaai_be.domain.risk.policy.ProgressBehindRule;
import org.wonik.coreviaai_be.domain.risk.service.result.DetectedRisk;
import org.wonik.coreviaai_be.domain.wbs.entity.Milestone;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsDependency;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.repository.MilestoneRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.MilestoneTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsDependencyRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.service.result.MilestoneLinkedTask;

import lombok.RequiredArgsConstructor;

/**
 * 한 프로젝트의 일정 지연 감지(FR-AIR-01). 규칙만 적용하고 LLM은 쓰지 않는다.
 * 같은 대상에 사유가 여러 개면 각각 한 행으로 남긴다.
 */
@Service
@RequiredArgsConstructor
public class DelayDetectionService {

	private final WbsTaskRepository taskRepository;
	private final WbsDependencyRepository dependencyRepository;
	private final MilestoneRepository milestoneRepository;
	private final MilestoneTaskRepository milestoneTaskRepository;
	private final RiskAnalysisProperties properties;

	public List<DetectedRisk> detectForProject(final Long projectId, final LocalDate today) {
		final List<WbsTask> tasks = taskRepository.findAllByProjectIdOrderBySortOrderAscIdAsc(projectId);
		final Map<Long, WbsTask> taskById = tasks.stream()
			.collect(Collectors.toMap(WbsTask::getId, Function.identity()));
		final List<DetectedRisk> detections = new ArrayList<>();

		for (final WbsTask task : tasks) {
			OverdueRule.evaluate(projectId, task.getId(), task.getStatus(), task.getEndDate(), today)
				.ifPresent(detections::add);
			ProgressBehindRule.evaluate(
					projectId,
					task.getId(),
					task.getStatus(),
					task.getProgress(),
					task.getStartDate(),
					task.getEndDate(),
					today,
					properties.progressBehindMarginPercent())
				.ifPresent(detections::add);
		}

		final List<WbsDependency> dependencies = dependencyRepository.findAllByProjectIdOrderByIdAsc(projectId);
		// 후행당 가장 긴 선행 지연만 남긴다(같은 사유 중복 행 방지).
		final Map<Long, DetectedRisk> predecessorBySuccessor = new HashMap<>();
		for (final WbsDependency dependency : dependencies) {
			final WbsTask successor = taskById.get(dependency.getSuccessorTaskId());
			final WbsTask predecessor = taskById.get(dependency.getPredecessorTaskId());
			if (successor == null || predecessor == null) {
				continue;
			}
			PredecessorDelayRule.evaluate(
					projectId,
					successor.getId(),
					successor.getStatus(),
					predecessor.getId(),
					predecessor.getStatus(),
					predecessor.getEndDate(),
					today)
				.ifPresent(detected -> predecessorBySuccessor.merge(
					detected.targetId(),
					detected,
					(left, right) -> left.metricValue().compareTo(right.metricValue()) >= 0 ? left : right));
		}
		detections.addAll(predecessorBySuccessor.values());

		final List<Milestone> milestones = milestoneRepository.findAllByProjectIdOrderByTargetDateAscIdAsc(projectId);
		if (!milestones.isEmpty()) {
			final List<Long> milestoneIds = milestones.stream().map(Milestone::getId).toList();
			final Map<Long, List<MilestoneLinkedTask>> linkedByMilestone = milestoneTaskRepository
				.findLinkedTasks(milestoneIds)
				.stream()
				.collect(Collectors.groupingBy(MilestoneLinkedTask::milestoneId));
			for (final Milestone milestone : milestones) {
				MilestoneAtRiskRule.evaluate(
						projectId,
						milestone.getId(),
						milestone.getTargetDate(),
						today,
						linkedByMilestone.getOrDefault(milestone.getId(), List.of()))
					.ifPresent(detections::add);
			}
		}
		return detections;
	}
}
