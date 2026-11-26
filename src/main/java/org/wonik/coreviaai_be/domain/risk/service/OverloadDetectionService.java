package org.wonik.coreviaai_be.domain.risk.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskReasonCode;
import org.wonik.coreviaai_be.domain.risk.entity.type.RiskTargetType;
import org.wonik.coreviaai_be.domain.risk.policy.OverloadAllocationPolicy;
import org.wonik.coreviaai_be.domain.risk.policy.OverloadAllocationPolicy.Hit;
import org.wonik.coreviaai_be.domain.risk.policy.OverloadAllocationPolicy.TaskLoad;
import org.wonik.coreviaai_be.domain.risk.service.result.DetectedRisk;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;

import lombok.RequiredArgsConstructor;

/**
 * 한 프로젝트의 인력 편중 감지(FR-AIR-03). 판정은 {@link OverloadAllocationPolicy}에 두고 여기서는 작업을 읽기만 한다.
 */
@Service
@RequiredArgsConstructor
public class OverloadDetectionService {

	private final WbsTaskRepository taskRepository;

	public List<DetectedRisk> detectForProject(final Long projectId, final int thresholdPercent) {
		final List<TaskLoad> loads = taskRepository.findAllByProjectIdOrderBySortOrderAscIdAsc(projectId).stream()
			.map(OverloadDetectionService::toLoad)
			.toList();
		return OverloadAllocationPolicy.evaluate(loads, thresholdPercent).stream()
			.map(hit -> toDetected(projectId, hit))
			.toList();
	}

	private static TaskLoad toLoad(final WbsTask task) {
		return new TaskLoad(
			task.getAssigneeUserId(),
			task.getId(),
			task.getParentTaskId(),
			task.getStatus(),
			task.getEstimatedEffort(),
			task.getStartDate(),
			task.getEndDate());
	}

	private static DetectedRisk toDetected(final Long projectId, final Hit hit) {
		return new DetectedRisk(
			projectId,
			RiskReasonCode.OVER_ALLOCATION,
			RiskTargetType.USER,
			hit.userId(),
			hit.allocationPercent(),
			hit.detailsJson());
	}
}
