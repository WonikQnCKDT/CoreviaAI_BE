package org.wonik.coreviaai_be.domain.project.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.project.dto.request.CreateProjectRequest;
import org.wonik.coreviaai_be.domain.project.entity.ProjectPhase;
import org.wonik.coreviaai_be.domain.project.policy.ProjectAccessPolicy;
import org.wonik.coreviaai_be.domain.project.repository.ProjectPhaseRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditChanges;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/** 프로젝트별 사용자 정의 단계의 추가·이름 변경·순서 변경·삭제. 모든 변경은 PM만 할 수 있다. */
@Service
@RequiredArgsConstructor
public class ProjectPhaseService {

	private final ProjectAccessPolicy accessPolicy;
	private final ProjectPhaseRepository phaseRepository;
	private final WbsTaskRepository taskRepository;
	private final AuditLogger auditLogger;

	@Transactional
	public ProjectPhase create(final AuditActor actor, final Long projectId, final String name) {
		accessPolicy.getManagedProjectForUpdate(actor.userId(), projectId);
		final long count = phaseRepository.countByProjectId(projectId);
		if (count >= CreateProjectRequest.MAX_PHASES) {
			throw new CustomException(ErrorCode.PROJECT_PHASE_LIMIT_EXCEEDED);
		}
		final ProjectPhase phase = phaseRepository.save(ProjectPhase.of(projectId, name.strip(), (int) count + 1, null));
		auditLogger.record(actor, AuditAction.PROJECT_PHASE_CREATED, AuditTargetType.PROJECT, projectId,
			AuditChanges.create().value("phaseId", phase.getId()).value("name", phase.getName())
				.value("phaseOrder", phase.getPhaseOrder()));
		return phase;
	}

	@Transactional
	public ProjectPhase rename(final AuditActor actor, final Long projectId, final Long phaseId, final String name) {
		accessPolicy.getManagedProjectForUpdate(actor.userId(), projectId);
		final ProjectPhase phase = getPhase(projectId, phaseId);
		final String normalized = name.strip();
		if (phase.getName().equals(normalized)) {
			return phase;
		}
		final String before = phase.getName();
		phase.rename(normalized);
		auditLogger.record(actor, AuditAction.PROJECT_PHASE_RENAMED, AuditTargetType.PROJECT, projectId,
			AuditChanges.create().value("phaseId", phaseId).change("name", before, normalized));
		return phase;
	}

	@Transactional
	public List<ProjectPhase> reorder(final AuditActor actor, final Long projectId, final List<Long> phaseIds) {
		accessPolicy.getManagedProjectForUpdate(actor.userId(), projectId);
		final List<ProjectPhase> phases = phaseRepository.findAllByProjectIdOrderByPhaseOrder(projectId);
		if (phaseIds.size() != phases.size() || new HashSet<>(phaseIds).size() != phaseIds.size()) {
			throw new CustomException(ErrorCode.INVALID_PROJECT_PHASE_ORDER);
		}
		final Map<Long, ProjectPhase> byId = phases.stream().collect(java.util.stream.Collectors.toMap(
			ProjectPhase::getId, phase -> phase));
		if (!byId.keySet().equals(new HashSet<>(phaseIds))) {
			throw new CustomException(ErrorCode.INVALID_PROJECT_PHASE_ORDER);
		}
		for (int index = 0; index < phaseIds.size(); index++) {
			byId.get(phaseIds.get(index)).reorder(phases.size() + index + 1);
		}
		phaseRepository.flush();
		for (int index = 0; index < phaseIds.size(); index++) {
			byId.get(phaseIds.get(index)).reorder(index + 1);
		}
		phaseRepository.flush();
		auditLogger.record(actor, AuditAction.PROJECT_PHASE_REORDERED, AuditTargetType.PROJECT, projectId,
			AuditChanges.create().value("phaseIds", phaseIds));
		return phaseIds.stream().map(byId::get).toList();
	}

	@Transactional
	public void delete(final AuditActor actor, final Long projectId, final Long phaseId) {
		accessPolicy.getManagedProjectForUpdate(actor.userId(), projectId);
		final ProjectPhase phase = getPhase(projectId, phaseId);
		if (taskRepository.existsByPhaseId(phaseId)) {
			throw new CustomException(ErrorCode.PROJECT_PHASE_IN_USE);
		}
		final String name = phase.getName();
		final int removedOrder = phase.getPhaseOrder();
		phaseRepository.delete(phase);
		phaseRepository.flush();
		final List<ProjectPhase> remaining = phaseRepository.findAllByProjectIdOrderByPhaseOrder(projectId);
		final int temporaryOrderBase = remaining.stream()
			.mapToInt(ProjectPhase::getPhaseOrder)
			.max()
			.orElse(0);
		for (int index = 0; index < remaining.size(); index++) {
			remaining.get(index).reorder(temporaryOrderBase + index + 1);
		}
		phaseRepository.flush();
		for (int index = 0; index < remaining.size(); index++) {
			remaining.get(index).reorder(index + 1);
		}
		phaseRepository.flush();
		auditLogger.record(actor, AuditAction.PROJECT_PHASE_DELETED, AuditTargetType.PROJECT, projectId,
			AuditChanges.create().value("phaseId", phaseId).value("name", name).value("phaseOrder", removedOrder));
	}

	private ProjectPhase getPhase(final Long projectId, final Long phaseId) {
		return phaseRepository.findByIdAndProjectId(phaseId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_PHASE_NOT_FOUND));
	}
}
