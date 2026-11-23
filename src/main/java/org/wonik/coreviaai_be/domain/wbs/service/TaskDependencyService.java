package org.wonik.coreviaai_be.domain.wbs.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsDependency;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsDependencyCycleValidator;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsEditor;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsTaskAccessPolicy;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsDependencyRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.service.command.ReplaceTaskPredecessorsCommand;
import org.wonik.coreviaai_be.domain.wbs.service.result.TaskDependencies;
import org.wonik.coreviaai_be.domain.wbs.service.result.TaskDependencyEdge;
import org.wonik.coreviaai_be.domain.wbs.service.result.TaskPredecessor;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.time.BusinessCalendar;

import lombok.RequiredArgsConstructor;

/** 완료→시작 선후행 관계(FR-WBS-03)의 조회·전체 교체와 순환 검증. */
@Service
@RequiredArgsConstructor
public class TaskDependencyService {

	/** 한 후행 작업에 허용하는 선행 작업 수. 프로젝트 WBS 전체 조회와 요청 크기를 제한한다. */
	public static final int MAX_PREDECESSORS_PER_TASK = 200;

	private final WbsTaskAccessPolicy accessPolicy;
	private final WbsTaskRepository taskRepository;
	private final WbsDependencyRepository dependencyRepository;
	private final WbsDependencyCycleValidator cycleValidator;
	private final Clock clock;

	@Transactional(readOnly = true)
	public TaskDependencies find(final Long userId, final Long projectId, final Long taskId) {
		accessPolicy.getReadableProject(userId, projectId);
		final WbsTask successor = getTask(projectId, taskId);
		return view(successor);
	}

	/**
	 * 보낸 목록을 후행 작업의 새 선행 작업 전체로 사용한다. 프로젝트 행 잠금으로 같은 프로젝트의 다른 WBS 변경과
	 * 직렬화하고, 실제 변경이 없으면 저장하지 않는 멱등 요청이다.
	 */
	@Transactional
	public TaskDependencies replace(
		final AuditActor actor,
		final Long projectId,
		final Long taskId,
		final ReplaceTaskPredecessorsCommand command
	) {
		final WbsEditor editor = accessPolicy.getEditor(actor.userId(), projectId);
		final WbsTask successor = getTask(projectId, taskId);
		editor.requireCanUpdate(successor);
		final Set<Long> requested = command.predecessorTaskIds();
		validatePredecessors(projectId, taskId, requested);

		final List<WbsDependency> currentLinks = dependencyRepository
			.findAllBySuccessorTaskIdOrderByPredecessorTaskIdAsc(taskId);
		final Set<Long> current = currentLinks.stream()
			.map(WbsDependency::getPredecessorTaskId)
			.collect(Collectors.toSet());
		if (current.equals(requested)) {
			return view(successor);
		}

		final List<TaskDependencyEdge> proposed = dependencyRepository.findAllByProjectIdOrderByIdAsc(projectId).stream()
			.filter(link -> !link.getSuccessorTaskId().equals(taskId))
			.map(link -> new TaskDependencyEdge(link.getPredecessorTaskId(), link.getSuccessorTaskId()))
			.collect(Collectors.toCollection(ArrayList::new));
		requested.forEach(predecessorId -> proposed.add(new TaskDependencyEdge(predecessorId, taskId)));
		cycleValidator.validate(proposed);

		dependencyRepository.deleteAll(currentLinks.stream()
			.filter(link -> !requested.contains(link.getPredecessorTaskId()))
			.toList());
		dependencyRepository.saveAll(requested.stream()
			.filter(predecessorId -> !current.contains(predecessorId))
			.map(predecessorId -> WbsDependency.finishToStart(projectId, predecessorId, taskId, actor.userId()))
			.toList());
		dependencyRepository.flush();
		return view(successor);
	}

	private void validatePredecessors(final Long projectId, final Long successorTaskId, final Set<Long> predecessors) {
		if (predecessors.contains(successorTaskId)
			|| (!predecessors.isEmpty() && taskRepository.countByProjectIdAndIdIn(projectId, predecessors) != predecessors.size())) {
			throw new CustomException(ErrorCode.INVALID_WBS_DEPENDENCY);
		}
	}

	private WbsTask getTask(final Long projectId, final Long taskId) {
		return taskRepository.findByIdAndProjectId(taskId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.WBS_TASK_NOT_FOUND));
	}

	/** 선행 작업은 한 번에 읽고, 지연은 업무 시간대의 현재 날짜 기준으로 계산한다. */
	private TaskDependencies view(final WbsTask successor) {
		final Set<Long> predecessorIds = dependencyRepository
			.findAllBySuccessorTaskIdOrderByPredecessorTaskIdAsc(successor.getId()).stream()
			.map(WbsDependency::getPredecessorTaskId)
			.collect(Collectors.toCollection(HashSet::new));
		if (predecessorIds.isEmpty()) {
			return new TaskDependencies(successor.getId(), List.of());
		}
		final LocalDate today = BusinessCalendar.today(clock);
		final List<TaskPredecessor> predecessors = taskRepository
			.findAllByProjectIdAndIdInOrderByIdAsc(successor.getProjectId(), predecessorIds).stream()
			.map(task -> predecessor(task, successor, today))
			.toList();
		return new TaskDependencies(successor.getId(), predecessors);
	}

	private static TaskPredecessor predecessor(
		final WbsTask predecessor,
		final WbsTask successor,
		final LocalDate today
	) {
		final boolean delayed = predecessor.getStatus() != WbsTaskStatus.DONE
			&& predecessor.getEndDate().isBefore(today);
		final long delayDays = delayed ? ChronoUnit.DAYS.between(predecessor.getEndDate(), today) : 0;
		return new TaskPredecessor(predecessor.getId(), predecessor.getParentTaskId(), predecessor.getName(),
			predecessor.getStatus(), predecessor.getEndDate(), delayed, delayDays,
			delayed && successor.getStatus() != WbsTaskStatus.DONE);
	}
}
