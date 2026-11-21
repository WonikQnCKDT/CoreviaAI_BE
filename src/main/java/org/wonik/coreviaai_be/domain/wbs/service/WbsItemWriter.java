package org.wonik.coreviaai_be.domain.wbs.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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
import org.wonik.coreviaai_be.domain.wbs.service.command.ApplyWbsDraftCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftMilestoneCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftSubtaskCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftTaskCommand;
import org.wonik.coreviaai_be.domain.wbs.service.result.AppliedWbsDraft;

import lombok.RequiredArgsConstructor;

/**
 * 검증을 마친 AI 초안 항목을 WBS에 등록한다(FR-PRJ-07 반영). 호출자의 트랜잭션 안에서만 실행해
 * 작업·하위작업·선후행·마일스톤이 함께 저장되거나 함께 취소되게 한다(MANDATORY).
 * <p>
 * 새 작업은 프로젝트의 기존 작업 뒤에 요청 순서대로 붙이고, 하위작업은 상위 작업 안에서 1부터 매긴다.
 * 하위작업이 있는 작업의 기간은 하위작업 기간(최소 시작일~최대 종료일)으로 맞춘다(FR-WBS-01 상위 기간 자동 산출).
 * 등록한 작업·하위작업마다 CREATED 이력을 남긴다(FR-TSK-01, 상위 기간 산출이 끝난 값으로). 유형·우선순위는 기본값.
 */
@Component
@RequiredArgsConstructor
public class WbsItemWriter {

	private final WbsTaskRepository taskRepository;
	private final WbsDependencyRepository dependencyRepository;
	private final MilestoneRepository milestoneRepository;
	private final MilestoneTaskRepository milestoneTaskRepository;
	private final TaskHistoryRecorder historyRecorder;

	@Transactional(propagation = Propagation.MANDATORY)
	public AppliedWbsDraft writeAiDraft(
		final Long projectId,
		final Long userId,
		final ApplyWbsDraftCommand command
	) {
		final Map<String, WbsTask> tasks = saveTasks(projectId, userId, command.tasks());
		final Map<String, Long> taskIds = new LinkedHashMap<>();
		tasks.forEach((tempId, task) -> taskIds.put(tempId, task.getId()));
		final List<WbsTask> subtasks = saveSubtasks(userId, tasks, command.tasks(), taskIds);
		final List<WbsTask> created = new ArrayList<>(tasks.values());
		created.addAll(subtasks);
		historyRecorder.created(userId, created);
		saveDependencies(projectId, userId, taskIds, command.tasks());
		final Map<String, Long> milestoneIds = saveMilestones(projectId, userId, taskIds, command.milestones());
		return new AppliedWbsDraft(taskIds, milestoneIds);
	}

	private Map<String, WbsTask> saveTasks(
		final Long projectId,
		final Long userId,
		final List<DraftTaskCommand> commands
	) {
		int sortOrder = taskRepository.findLastTopLevelSortOrder(projectId);

		final Map<String, WbsTask> tasks = new LinkedHashMap<>();
		for (final DraftTaskCommand command : commands) {
			final TaskDetails details = new TaskDetails(command.name(), command.description(), command.assigneeUserId(),
				command.startDate(), command.endDate(), command.estimatedPersonDays());
			tasks.put(command.tempId(),
				WbsTask.task(projectId, ++sortOrder, details, WbsItemSource.AI_DRAFT, userId));
		}
		taskRepository.saveAll(tasks.values());
		return tasks;
	}

	/** 하위작업을 저장하고 tempId → ID를 taskIds에 더한다. 저장한 하위작업을 돌려준다. */
	private List<WbsTask> saveSubtasks(
		final Long userId,
		final Map<String, WbsTask> tasks,
		final List<DraftTaskCommand> commands,
		final Map<String, Long> taskIds
	) {
		final Map<String, WbsTask> subtasks = new LinkedHashMap<>();
		for (final DraftTaskCommand command : commands) {
			final WbsTask parent = tasks.get(command.tempId());
			final List<WbsTask> children = new ArrayList<>();
			int sortOrder = 0;
			for (final DraftSubtaskCommand subtask : command.subtasks()) {
				final TaskDetails details = new TaskDetails(subtask.name(), subtask.description(), subtask.assigneeUserId(),
					subtask.startDateOr(command.startDate()), subtask.endDateOr(command.endDate()),
					subtask.estimatedPersonDays());
				final WbsTask child = WbsTask.subtask(parent, ++sortOrder, details, WbsItemSource.AI_DRAFT, userId);
				subtasks.put(subtask.tempId(), child);
				children.add(child);
			}
			parent.deriveScheduleFrom(children);
		}
		taskRepository.saveAll(subtasks.values());
		subtasks.forEach((tempId, subtask) -> taskIds.put(tempId, subtask.getId()));
		return List.copyOf(subtasks.values());
	}

	private void saveDependencies(
		final Long projectId,
		final Long userId,
		final Map<String, Long> taskIds,
		final List<DraftTaskCommand> commands
	) {
		final List<WbsDependency> dependencies = new ArrayList<>();
		for (final DraftTaskCommand command : commands) {
			final Long successorId = taskIds.get(command.tempId());
			new LinkedHashSet<>(command.dependencies()).forEach(predecessor ->
				dependencies.add(WbsDependency.finishToStart(projectId, taskIds.get(predecessor), successorId, userId)));
		}
		dependencyRepository.saveAll(dependencies);
	}

	private Map<String, Long> saveMilestones(
		final Long projectId,
		final Long userId,
		final Map<String, Long> taskIds,
		final List<DraftMilestoneCommand> commands
	) {
		final Map<String, Long> milestoneIds = new LinkedHashMap<>();
		final List<MilestoneTask> links = new ArrayList<>();
		for (final DraftMilestoneCommand command : commands) {
			final Milestone milestone = milestoneRepository.save(
				Milestone.of(projectId, command.name(), command.targetDate(), WbsItemSource.AI_DRAFT, userId));
			milestoneIds.put(command.tempId(), milestone.getId());
			new LinkedHashSet<>(command.relatedTaskIds()).forEach(tempId ->
				links.add(MilestoneTask.of(milestone.getId(), taskIds.get(tempId))));
		}
		milestoneTaskRepository.saveAll(links);
		return milestoneIds;
	}
}
