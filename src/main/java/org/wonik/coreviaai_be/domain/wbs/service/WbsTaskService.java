package org.wonik.coreviaai_be.domain.wbs.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskDetails;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsEditor;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsTaskAccessPolicy;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsTaskRules;
import org.wonik.coreviaai_be.domain.wbs.repository.MilestoneTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsDependencyRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.service.command.CreateWbsTaskCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.MoveWbsTaskCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.UpdateWbsTaskCommand;
import org.wonik.coreviaai_be.domain.wbs.service.result.WbsTaskChange;
import org.wonik.coreviaai_be.domain.wbs.service.result.WbsTreeNode;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditChanges;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 계층형 WBS(FR-WBS-01): 트리 조회, 작업·하위작업 등록·수정·이동·삭제.
 * <p>
 * 계층은 작업(parent NULL) > 하위작업 2단계다(04. WBS·일정 ERD). 하위작업 아래에는 만들거나 옮길 수 없다.
 * 하위작업이 바뀌면(등록, 기간 수정, 이동, 삭제) 같은 트랜잭션에서 상위 작업 기간을 하위작업 기간으로 다시 맞춘다.
 * <p>
 * 동시성: 모든 변경은 프로젝트 행을 잠그고 시작한다(WbsTaskAccessPolicy#getEditor). 수정·이동은 요청의 version이
 * 현재 값과 다르면 409 CONCURRENT_MODIFICATION으로 거부한다. 같은 결과가 이미 반영된 재전송(같은 값 수정, 같은 위치 이동)은
 * version을 보지 않고 그대로 성공한다(멱등).
 * <p>
 * 상태(status)는 등록 시 TODO이고 이 API로 바꾸지 않는다(칸반 FR-TSK-02). 업무 상세 필드·변경 이력은 FR-TSK-01이 맡는다.
 */
@Service
@RequiredArgsConstructor
public class WbsTaskService {

	/** 프로젝트당 WBS 항목(작업 + 하위작업) 상한. NFR-02(작업 1,000건 렌더링)의 2배로, 트리를 한 번에 돌려줄 수 있는 크기다. */
	public static final int MAX_ITEMS_PER_PROJECT = 2000;

	private final WbsTaskAccessPolicy accessPolicy;
	private final WbsTaskRules rules;
	private final WbsTaskRepository taskRepository;
	private final WbsDependencyRepository dependencyRepository;
	private final MilestoneTaskRepository milestoneTaskRepository;
	private final WbsTreeReader treeReader;
	private final AuditLogger auditLogger;

	@Transactional(readOnly = true)
	public List<WbsTreeNode> findTree(final Long userId, final Long projectId) {
		accessPolicy.getReadableProject(userId, projectId);
		return treeReader.read(projectId);
	}

	/** 같은 부모의 맨 뒤에 붙인다. 출처는 MANUAL, 상태는 TODO. */
	@Transactional
	public WbsTaskChange create(final AuditActor actor, final Long projectId, final CreateWbsTaskCommand command) {
		final WbsEditor editor = accessPolicy.getEditor(actor.userId(), projectId);
		final TaskDetails details = command.details();
		WbsTask parent = null;
		if (command.parentTaskId() == null) {
			editor.requirePm();
		} else {
			parent = getTask(projectId, command.parentTaskId());
			editor.requireCanAddSubtaskUnder(parent);
			if (parent.isSubtask()) {
				throw new CustomException(ErrorCode.INVALID_WBS_HIERARCHY);
			}
		}
		editor.requireCanAssignOnCreate(details.assigneeUserId());
		rules.validateSchedule(editor.project(), details.startDate(), details.endDate());
		rules.validateAssignee(projectId, details.assigneeUserId());
		if (taskRepository.countByProjectId(projectId) >= MAX_ITEMS_PER_PROJECT) {
			throw new CustomException(ErrorCode.WBS_TASK_LIMIT_EXCEEDED);
		}

		final WbsTask task = parent == null
			? WbsTask.task(projectId, taskRepository.findLastTopLevelSortOrder(projectId) + 1, details,
			WbsItemSource.MANUAL, actor.userId())
			: WbsTask.subtask(parent, taskRepository.findLastSortOrderUnder(parent.getId()) + 1, details,
			WbsItemSource.MANUAL, actor.userId());
		taskRepository.saveAndFlush(task);
		if (parent != null) {
			deriveSchedule(parent);
		}
		return changed(task.getId(), projectId);
	}

	/**
	 * 기본 필드를 보낸 값으로 덮어쓴다. 판정 순서: 404 → 403(담당 여부) → 같은 값이면 그대로 성공 → 409(version)
	 * → 403(담당자 변경은 PM만) → 400(값 규칙). 담당자는 바꿀 때만 검증한다(이미 배정된 사람이 나중에 비활성이 돼도 다른 필드 수정은 가능).
	 */
	@Transactional
	public WbsTaskChange update(
		final AuditActor actor,
		final Long projectId,
		final Long taskId,
		final UpdateWbsTaskCommand command
	) {
		final WbsEditor editor = accessPolicy.getEditor(actor.userId(), projectId);
		final WbsTask task = getTask(projectId, taskId);
		editor.requireCanUpdate(task);
		final TaskDetails details = command.details();
		if (task.hasSameDetails(details)) {
			return changed(taskId, projectId);
		}
		requireVersion(task, command.version());

		if (!Objects.equals(task.getAssigneeUserId(), details.assigneeUserId())) {
			editor.requirePm();
			rules.validateAssignee(projectId, details.assigneeUserId());
		}
		final boolean scheduleChanged = !task.hasSameSchedule(details.startDate(), details.endDate());
		if (scheduleChanged) {
			if (taskRepository.existsByParentTaskId(taskId)) {
				throw new CustomException(ErrorCode.WBS_SCHEDULE_DERIVED);
			}
			rules.validateSchedule(editor.project(), details.startDate(), details.endDate());
		}

		task.changeDetails(details);
		taskRepository.flush();
		if (scheduleChanged && task.isSubtask()) {
			deriveSchedule(getTask(projectId, task.getParentTaskId()));
		}
		return changed(taskId, projectId);
	}

	/**
	 * 드래그 이동(PM만). 같은 부모 안 순서 변경, 다른 작업 아래로(작업 → 하위작업), 최상위로(하위작업 → 작업) 모두 이 API로 한다.
	 * 하위작업을 가진 작업은 다른 작업 아래로 옮길 수 없다(3단계 금지). 옮긴 뒤 옛 부모·새 부모의 형제 순서를 1부터 다시 매기고
	 * 두 상위 작업의 기간을 다시 산출한다. 형제 순서 변경은 version을 올리지 않는다(WbsTaskRepository#updateSortOrder).
	 */
	@Transactional
	public WbsTaskChange move(
		final AuditActor actor,
		final Long projectId,
		final Long taskId,
		final MoveWbsTaskCommand command
	) {
		final WbsEditor editor = accessPolicy.getEditor(actor.userId(), projectId);
		editor.requirePm();
		final WbsTask task = getTask(projectId, taskId);
		final Long newParentId = command.parentTaskId();
		WbsTask newParent = null;
		if (newParentId != null) {
			newParent = getTask(projectId, newParentId);
			if (newParent.getId().equals(taskId) || newParent.isSubtask() || taskRepository.existsByParentTaskId(taskId)) {
				throw new CustomException(ErrorCode.INVALID_WBS_HIERARCHY);
			}
		}

		final Long oldParentId = task.getParentTaskId();
		final boolean sameParent = Objects.equals(oldParentId, newParentId);
		final List<WbsTask> destination = siblings(projectId, newParentId);
		final int currentIndex = sameParent ? destination.indexOf(task) : -1;
		destination.remove(task);
		if (command.index() > destination.size()) {
			throw new CustomException(ErrorCode.INVALID_WBS_POSITION);
		}
		if (sameParent && currentIndex == command.index()) {
			return changed(taskId, projectId);
		}
		requireVersion(task, command.version());

		destination.add(command.index(), task);
		task.moveTo(newParentId, command.index() + 1);
		taskRepository.flush();
		final List<WbsTask> source = new ArrayList<>();
		if (!sameParent) {
			source.addAll(siblings(projectId, oldParentId));
			if (oldParentId != null) {
				deriveSchedule(getTask(projectId, oldParentId));
			}
			if (newParent != null) {
				deriveSchedule(newParent);
			}
			taskRepository.flush();
		}
		// 엔티티 변경을 모두 내보낸 뒤에 순서만 일괄 갱신한다(일괄 갱신은 영속성 컨텍스트를 비운다).
		renumber(destination);
		renumber(source);
		return changed(taskId, projectId);
	}

	/**
	 * 삭제(PM만, 3.2 공통 규칙 '업무 삭제는 PM만'). 삭제 정책이 정해지지 않아(04. WBS·일정 ERD 3절) 되돌리기 쉬운 쪽으로,
	 * 하위작업·선후행 관계·마일스톤 연결이 하나라도 있으면 409 WBS_TASK_IN_USE로 거부한다(연쇄 삭제 없음). 감사 로그를 남긴다.
	 */
	@Transactional
	public WbsTaskChange delete(final AuditActor actor, final Long projectId, final Long taskId) {
		final WbsEditor editor = accessPolicy.getEditor(actor.userId(), projectId);
		editor.requirePm();
		final WbsTask task = getTask(projectId, taskId);
		if (taskRepository.existsByParentTaskId(taskId)
			|| dependencyRepository.existsByPredecessorTaskIdOrSuccessorTaskId(taskId, taskId)
			|| milestoneTaskRepository.existsByTaskId(taskId)) {
			throw new CustomException(ErrorCode.WBS_TASK_IN_USE);
		}

		final Long parentId = task.getParentTaskId();
		taskRepository.delete(task);
		taskRepository.flush();
		if (parentId != null) {
			deriveSchedule(getTask(projectId, parentId));
		}
		auditLogger.record(actor, AuditAction.WBS_TASK_DELETED, AuditTargetType.WBS_TASK, taskId,
			AuditChanges.create()
				.value("projectId", projectId)
				.value("parentTaskId", parentId)
				.value("name", task.getName()));
		return changed(taskId, projectId);
	}

	private WbsTask getTask(final Long projectId, final Long taskId) {
		return taskRepository.findByIdAndProjectId(taskId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.WBS_TASK_NOT_FOUND));
	}

	private static void requireVersion(final WbsTask task, final long version) {
		if (task.getVersion() != version) {
			throw new CustomException(ErrorCode.CONCURRENT_MODIFICATION);
		}
	}

	/** 같은 부모의 형제 목록(표시 순서). parentTaskId가 null이면 최상위 작업. 수정 가능한 새 목록을 돌려준다. */
	private List<WbsTask> siblings(final Long projectId, final Long parentTaskId) {
		return new ArrayList<>(parentTaskId == null
			? taskRepository.findAllByProjectIdAndParentTaskIdIsNullOrderBySortOrderAscIdAsc(projectId)
			: taskRepository.findAllByParentTaskIdOrderBySortOrderAscIdAsc(parentTaskId));
	}

	private void deriveSchedule(final WbsTask parent) {
		parent.deriveScheduleFrom(taskRepository.findAllByParentTaskIdOrderBySortOrderAscIdAsc(parent.getId()));
	}

	/** 목록 순서대로 1부터 다시 매긴다. 이미 맞는 항목은 건드리지 않는다. */
	private void renumber(final List<WbsTask> ordered) {
		for (int i = 0; i < ordered.size(); i++) {
			final WbsTask task = ordered.get(i);
			if (task.getSortOrder() != i + 1) {
				taskRepository.updateSortOrder(task.getId(), i + 1);
			}
		}
	}

	private WbsTaskChange changed(final Long taskId, final Long projectId) {
		taskRepository.flush();
		return new WbsTaskChange(taskId, treeReader.read(projectId));
	}
}
