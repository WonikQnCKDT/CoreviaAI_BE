package org.wonik.coreviaai_be.domain.wbs.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.wbs.entity.Milestone;
import org.wonik.coreviaai_be.domain.wbs.entity.MilestoneDeadline;
import org.wonik.coreviaai_be.domain.wbs.entity.MilestoneTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsEditor;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsTaskAccessPolicy;
import org.wonik.coreviaai_be.domain.wbs.repository.MilestoneRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.MilestoneTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.service.command.CreateMilestoneCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.ReplaceMilestoneTasksCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.UpdateMilestoneCommand;
import org.wonik.coreviaai_be.domain.wbs.service.result.MilestoneLinkedTask;
import org.wonik.coreviaai_be.domain.wbs.service.result.MilestoneView;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditChanges;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.time.BusinessCalendar;

import lombok.RequiredArgsConstructor;

/**
 * 마일스톤(FR-WBS-04): 목록·상세 조회(D-day·지연 경고 포함), 등록·수정·삭제, 연결 작업 설정.
 * <p>
 * 권한(3.3 WBS-04): 조회는 프로젝트를 볼 수 있는 사용자 전원, 변경은 PM만. 판정 순서와 프로젝트 행 잠금은
 * 계층형 WBS(WbsTaskService)와 같다. 작업 삭제가 마일스톤 연결을 확인하므로(WBS_TASK_IN_USE) 연결 변경도 같은 잠금 안에서 한다.
 * <p>
 * D-day·지연 경고는 저장하지 않고 조회 때 업무 시간대(BusinessCalendar) 기준 오늘로 계산한다(MilestoneDeadline).
 */
@Service
@RequiredArgsConstructor
public class MilestoneService {

	/** 프로젝트당 마일스톤 상한. 목록을 페이지 없이 한 번에 돌려주기 위한 크기다(AI 초안 1회 반영 최대 50개의 4배). */
	public static final int MAX_MILESTONES_PER_PROJECT = 200;
	/** 마일스톤 하나에 연결할 수 있는 작업 수(AI 초안 반영의 relatedTaskIds 상한과 같음). */
	public static final int MAX_TASKS_PER_MILESTONE = 200;

	private final WbsTaskAccessPolicy accessPolicy;
	private final MilestoneRepository milestoneRepository;
	private final MilestoneTaskRepository milestoneTaskRepository;
	private final WbsTaskRepository taskRepository;
	private final AuditLogger auditLogger;
	private final Clock clock;

	@Transactional(readOnly = true)
	public List<MilestoneView> findAll(final Long userId, final Long projectId) {
		accessPolicy.getReadableProject(userId, projectId);
		return views(milestoneRepository.findAllByProjectIdOrderByTargetDateAscIdAsc(projectId));
	}

	@Transactional(readOnly = true)
	public MilestoneView find(final Long userId, final Long projectId, final Long milestoneId) {
		accessPolicy.getReadableProject(userId, projectId);
		return view(getMilestone(projectId, milestoneId));
	}

	@Transactional
	public MilestoneView create(final AuditActor actor, final Long projectId, final CreateMilestoneCommand command) {
		final WbsEditor editor = accessPolicy.getEditor(actor.userId(), projectId);
		editor.requirePm();
		validateTargetDate(editor.project(), command.targetDate());
		validateTasks(projectId, command.taskIds());
		if (milestoneRepository.countByProjectId(projectId) >= MAX_MILESTONES_PER_PROJECT) {
			throw new CustomException(ErrorCode.MILESTONE_LIMIT_EXCEEDED);
		}

		final Milestone milestone = milestoneRepository.save(
			Milestone.of(projectId, command.name(), command.targetDate(), WbsItemSource.MANUAL, actor.userId()));
		milestoneTaskRepository.saveAll(
			command.taskIds().stream().map(taskId -> MilestoneTask.of(milestone.getId(), taskId)).toList());
		milestoneTaskRepository.flush();
		return view(milestone);
	}

	/**
	 * 이름·목표일 수정. 판정 순서: 404 → 403 → 같은 값이면 그대로 성공(멱등) → 409(version) → 400(목표일 규칙).
	 */
	@Transactional
	public MilestoneView update(
		final AuditActor actor,
		final Long projectId,
		final Long milestoneId,
		final UpdateMilestoneCommand command
	) {
		final WbsEditor editor = accessPolicy.getEditor(actor.userId(), projectId);
		editor.requirePm();
		final Milestone milestone = getMilestone(projectId, milestoneId);
		if (milestone.hasSameDetails(command.name(), command.targetDate())) {
			return view(milestone);
		}
		requireVersion(milestone, command.version());
		validateTargetDate(editor.project(), command.targetDate());

		milestone.changeDetails(command.name(), command.targetDate());
		milestoneRepository.flush();
		return view(milestone);
	}

	/**
	 * 연결 작업 교체. 보낸 집합이 새 연결 전체다: 빠진 연결은 지우고 새 연결만 추가한다(남는 연결은 그대로).
	 * 같은 집합이면 version을 보지 않고 그대로 성공한다(멱등). 바뀌면 마일스톤 version을 올린다.
	 */
	@Transactional
	public MilestoneView replaceTasks(
		final AuditActor actor,
		final Long projectId,
		final Long milestoneId,
		final ReplaceMilestoneTasksCommand command
	) {
		final WbsEditor editor = accessPolicy.getEditor(actor.userId(), projectId);
		editor.requirePm();
		final Milestone milestone = getMilestone(projectId, milestoneId);
		final Set<Long> current = milestoneTaskRepository.findAllByMilestoneId(milestoneId).stream()
			.map(MilestoneTask::getTaskId)
			.collect(Collectors.toSet());
		final Set<Long> requested = command.taskIds();
		if (current.equals(requested)) {
			return view(milestone);
		}
		requireVersion(milestone, command.version());
		validateTasks(projectId, requested);

		final Set<Long> removed = new HashSet<>(current);
		removed.removeAll(requested);
		if (!removed.isEmpty()) {
			milestoneTaskRepository.deleteLinks(milestoneId, removed);
		}
		milestoneTaskRepository.saveAll(requested.stream()
			.filter(taskId -> !current.contains(taskId))
			.map(taskId -> MilestoneTask.of(milestoneId, taskId))
			.toList());
		milestoneRepository.incrementVersion(milestoneId);
		return view(getMilestone(projectId, milestoneId));
	}

	/**
	 * 삭제(PM만, 3.2 공통 규칙 '마일스톤 삭제는 PM만'). 연결 정보(milestone_tasks)는 마일스톤에 딸린 데이터라 함께 지우고,
	 * 연결됐던 작업 자체는 남는다. 감사 로그를 남긴다.
	 */
	@Transactional
	public void delete(final AuditActor actor, final Long projectId, final Long milestoneId) {
		final WbsEditor editor = accessPolicy.getEditor(actor.userId(), projectId);
		editor.requirePm();
		final Milestone milestone = getMilestone(projectId, milestoneId);

		final int unlinked = milestoneTaskRepository.deleteAllByMilestoneId(milestoneId);
		milestoneRepository.delete(milestone);
		milestoneRepository.flush();
		auditLogger.record(actor, AuditAction.MILESTONE_DELETED, AuditTargetType.MILESTONE, milestoneId,
			AuditChanges.create()
				.value("projectId", projectId)
				.value("name", milestone.getName())
				.value("targetDate", milestone.getTargetDate().toString())
				.value("unlinkedTaskCount", unlinked));
	}

	private Milestone getMilestone(final Long projectId, final Long milestoneId) {
		return milestoneRepository.findByIdAndProjectId(milestoneId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.MILESTONE_NOT_FOUND));
	}

	private static void requireVersion(final Milestone milestone, final long version) {
		if (milestone.getVersion() != version) {
			throw new CustomException(ErrorCode.CONCURRENT_MODIFICATION);
		}
	}

	/** 프로젝트 기간이 있으면 그 안(시작일·종료일 중 있는 쪽만 확인, 작업 기간·AI 초안 반영과 같은 기준). 지난 날짜도 허용한다. */
	private static void validateTargetDate(final Project project, final LocalDate targetDate) {
		final boolean beforeStart = project.getStartDate() != null && targetDate.isBefore(project.getStartDate());
		final boolean afterEnd = project.getEndDate() != null && targetDate.isAfter(project.getEndDate());
		if (beforeStart || afterEnd) {
			throw new CustomException(ErrorCode.INVALID_MILESTONE_TARGET_DATE);
		}
	}

	/**
	 * 연결 작업은 모두 같은 프로젝트의 WBS 항목(작업·하위작업)이어야 한다. 없는 ID와 다른 프로젝트 ID를 구분하지 않고
	 * 같은 400으로 답해 다른 프로젝트 작업의 존재를 드러내지 않는다.
	 */
	private void validateTasks(final Long projectId, final Set<Long> taskIds) {
		if (!taskIds.isEmpty() && taskRepository.countByProjectIdAndIdIn(projectId, taskIds) != taskIds.size()) {
			throw new CustomException(ErrorCode.INVALID_MILESTONE_TASK);
		}
	}

	private MilestoneView view(final Milestone milestone) {
		return views(List.of(milestone)).get(0);
	}

	/**
	 * 연결 작업과 하위작업 상태를 각각 한 번의 조회로 읽는다(N+1 방지).
	 * 하위작업이 있는 상위 작업의 상태는 하위작업 상태에서 파생한다(FE wbsRules.deriveDisplayStatus와 같은 규칙).
	 * 오늘은 업무 시간대 기준으로 한 번만 정한다.
	 */
	private List<MilestoneView> views(final List<Milestone> milestones) {
		if (milestones.isEmpty()) {
			return List.of();
		}
		final Map<Long, List<MilestoneLinkedTask>> linked = new LinkedHashMap<>();
		milestones.forEach(milestone -> linked.put(milestone.getId(), new ArrayList<>()));
		final List<MilestoneLinkedTask> rawLinked = milestoneTaskRepository.findLinkedTasks(linked.keySet());
		final Map<Long, List<WbsTaskStatus>> subtaskStatusesByParent = loadSubtaskStatuses(rawLinked);
		for (final MilestoneLinkedTask task : rawLinked) {
			linked.get(task.milestoneId()).add(applyDerivedStatus(task, subtaskStatusesByParent));
		}

		final LocalDate today = BusinessCalendar.today(clock);
		return milestones.stream().map(milestone -> {
			final List<MilestoneLinkedTask> tasks = List.copyOf(linked.get(milestone.getId()));
			final MilestoneDeadline deadline = MilestoneDeadline.evaluate(milestone.getTargetDate(), today,
				tasks.stream().map(MilestoneLinkedTask::status).toList());
			return new MilestoneView(milestone, deadline, tasks);
		}).toList();
	}

	private Map<Long, List<WbsTaskStatus>> loadSubtaskStatuses(final List<MilestoneLinkedTask> linkedTasks) {
		final Set<Long> topLevelTaskIds = linkedTasks.stream()
			.filter(task -> task.parentTaskId() == null)
			.map(MilestoneLinkedTask::taskId)
			.collect(Collectors.toSet());
		if (topLevelTaskIds.isEmpty()) {
			return Map.of();
		}
		final Map<Long, List<WbsTaskStatus>> byParent = new HashMap<>();
		for (final WbsTaskRepository.SubtaskStatus row : taskRepository.findSubtaskStatusesByParentTaskIdIn(topLevelTaskIds)) {
			byParent.computeIfAbsent(row.getParentTaskId(), key -> new ArrayList<>()).add(row.getStatus());
		}
		return byParent;
	}

	private static MilestoneLinkedTask applyDerivedStatus(
		final MilestoneLinkedTask task,
		final Map<Long, List<WbsTaskStatus>> subtaskStatusesByParent
	) {
		if (task.parentTaskId() != null) {
			return task;
		}
		final List<WbsTaskStatus> subtaskStatuses = subtaskStatusesByParent.get(task.taskId());
		return task.withStatus(WbsTaskStatus.deriveFromSubtasks(task.status(), subtaskStatuses));
	}
}
