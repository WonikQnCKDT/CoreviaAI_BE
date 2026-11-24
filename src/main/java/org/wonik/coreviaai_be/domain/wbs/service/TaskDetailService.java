package org.wonik.coreviaai_be.domain.wbs.service;

import java.time.Clock;
import java.util.Arrays;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.wbs.entity.BoardColumn;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskHistory;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskSnapshot;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsEditor;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsTaskAccessPolicy;
import org.wonik.coreviaai_be.domain.wbs.repository.BoardColumnRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.TaskHistoryRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.service.command.UpdateTaskAttributesCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.UpdateTaskProgressCommand;
import org.wonik.coreviaai_be.domain.wbs.service.result.TaskDetail;
import org.wonik.coreviaai_be.domain.wbs.service.result.TaskHistoryView;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 업무 CRUD(FR-TSK-01) 중 WBS API(FR-WBS-01)에 없는 부분: 업무 상세 조회, 상세 필드(유형·우선순위·완료 조건) 수정,
 * 변경 이력 조회. 업무 = WBS 작업·하위작업(wbs_tasks)이라 등록·기본 필드·담당자 배정·이동·삭제는 WbsTaskService가 한다.
 * <p>
 * 권한(3.3 TSK-01): 조회는 프로젝트를 볼 수 있는 사용자 전원(멤버, 공개 범위 열람자, 소속 범위 경영진).
 * 상세 필드 수정은 PM, 그 업무를 담당한 개발자·QA(WBS 기본 필드 수정과 같은 규칙, {@link WbsEditor#requireCanUpdate}).
 * 기능 모듈은 WBS·칸반 중 하나만 켜져 있어도 된다(칸반 카드에서도 같은 업무 상세를 연다, SCRUM-109 미결 3 정리).
 * 판정 순서와 잠금은 WBS API와 같다(프로젝트 404 → 기능 404 → 업무 404 → 담당 403 → 같은 값이면 200 → version 409).
 */
@Service
@RequiredArgsConstructor
public class TaskDetailService {

	private final WbsTaskAccessPolicy accessPolicy;
    private final BoardColumnRepository columns;
    private final Clock clock;
	private final WbsTaskRepository taskRepository;
	private final TaskHistoryRepository historyRepository;
	private final TaskHistoryRecorder historyRecorder;
	private final UserNameReader userNameReader;
	private final JsonMapper jsonMapper;

	@Transactional(readOnly = true)
	public TaskDetail find(final Long userId, final Long projectId, final Long taskId) {
		accessPolicy.getReadableProject(userId, projectId, WbsTaskAccessPolicy.TASK_FEATURES);
		return detailOf(getTask(projectId, taskId));
	}

	@Transactional
	public TaskDetail updateAttributes(
		final AuditActor actor,
		final Long projectId,
		final Long taskId,
		final UpdateTaskAttributesCommand command
	) {
		final WbsEditor editor = accessPolicy.getEditor(actor.userId(), projectId, WbsTaskAccessPolicy.TASK_FEATURES);
		final WbsTask task = getTask(projectId, taskId);
		editor.requireCanUpdate(task);
		if (task.hasSameAttributes(command.attributes())) {
			return detailOf(task);
		}
		if (task.getVersion() != command.version()) {
			throw new CustomException(ErrorCode.CONCURRENT_MODIFICATION);
		}
		final TaskSnapshot before = TaskSnapshot.of(task);
		task.changeAttributes(command.attributes());
		taskRepository.flush();
		historyRecorder.changed(actor.userId(), before, TaskSnapshot.of(task));
		return detailOf(task);
	}

	@Transactional
	public TaskDetail updateProgress(
		final AuditActor actor,
		final Long projectId,
		final Long taskId,
		final UpdateTaskProgressCommand command
	) {
		final WbsEditor editor = accessPolicy.getEditor(actor.userId(), projectId, WbsTaskAccessPolicy.TASK_FEATURES);
		final WbsTask task = getTask(projectId, taskId);
		editor.requireCanUpdate(task);
		if (taskRepository.existsByParentTaskId(taskId)) {
			throw new CustomException(ErrorCode.WBS_PROGRESS_DERIVED);
		}
		if (task.getProgress() == command.progress()) {
			return detailOf(task);
		}
		if (task.getVersion() != command.version()) {
			throw new CustomException(ErrorCode.CONCURRENT_MODIFICATION);
		}
		if (task.getStatus() == WbsTaskStatus.DONE
			&& command.progress() != WbsTask.COMPLETED_PROGRESS) {
			throw new CustomException(ErrorCode.WBS_COMPLETED_PROGRESS_REQUIRED);
		}
		final TaskSnapshot before = TaskSnapshot.of(task);
		task.changeProgress(command.progress());
		taskRepository.flush();
		historyRecorder.changed(actor.userId(), before, TaskSnapshot.of(task));
		deriveParentProgress(actor.userId(), task);
		return detailOf(task);
	}

    @Transactional
    public TaskDetail complete(final AuditActor actor, final Long projectId, final Long taskId, final long version) {
        final WbsEditor editor = accessPolicy.getEditor(actor.userId(), projectId, WbsTaskAccessPolicy.TASK_FEATURES);
        final WbsTask task = getTask(projectId, taskId);
        editor.requireCanUpdate(task);
        if (taskRepository.existsByParentTaskId(taskId)) throw new CustomException(ErrorCode.WBS_PROGRESS_DERIVED);
        if (task.getStatus() == WbsTaskStatus.DONE) return detailOf(task);
        if (task.getVersion() != version) throw new CustomException(ErrorCode.CONCURRENT_MODIFICATION);
        final TaskSnapshot before = TaskSnapshot.of(task);
        final Long columnId = columns.findFirstByProjectIdAndStatusCategoryOrderBySortOrderAscIdAsc(projectId, WbsTaskStatus.DONE)
            .map(BoardColumn::getId).orElse(null);
        task.complete(columnId, clock.instant());
        taskRepository.flush();
        historyRecorder.changed(actor.userId(), before, TaskSnapshot.of(task));
        deriveParentProgress(actor.userId(), task);
        return detailOf(task);
    }

	/** 업무 변경 이력(커서, id 오름차순 = 시간순). 지운 업무의 이력은 남아 있지만 업무가 없으므로 404다. */
	@Transactional(readOnly = true)
	public CursorPage<TaskHistoryView> findHistories(
		final Long userId,
		final Long projectId,
		final Long taskId,
		final CursorPageRequest page
	) {
		accessPolicy.getReadableProject(userId, projectId, WbsTaskAccessPolicy.TASK_FEATURES);
		getTask(projectId, taskId);
		final CursorPage<TaskHistory> histories = CursorPage.of(
			historyRepository.findPageByTaskId(taskId, page.cursorId(), page.fetchPageable()),
			page.size(),
			TaskHistory::getId
		);
		final Map<Long, String> actorNames = userNameReader.namesOf(
			histories.items().stream().map(TaskHistory::getActorUserId).toList());
		return histories.map(history -> new TaskHistoryView(history, parse(history.getBeforeValue()),
			parse(history.getAfterValue()), actorNames.get(history.getActorUserId())));
	}

    private void deriveParentProgress(final Long userId, final WbsTask task) {
        if (!task.isSubtask()) return;
        final WbsTask parent = getTask(task.getProjectId(), task.getParentTaskId());
        final TaskSnapshot before = TaskSnapshot.of(parent);
        parent.changeProgress(ProgressCalculator.weightedAverage(
            taskRepository.findAllByParentTaskIdOrderBySortOrderAscIdAsc(parent.getId())));
        taskRepository.flush();
        historyRecorder.changed(userId, before, TaskSnapshot.of(parent));
    }

	private TaskDetail detailOf(final WbsTask task) {
		final Map<Long, String> names = userNameReader.namesOf(Arrays.asList(task.getAssigneeUserId(), task.getCreatedBy()));
		return new TaskDetail(task, names.get(task.getAssigneeUserId()), names.get(task.getCreatedBy()),
			taskRepository.existsByParentTaskId(task.getId()));
	}

	private WbsTask getTask(final Long projectId, final Long taskId) {
		return taskRepository.findByIdAndProjectId(taskId, projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.WBS_TASK_NOT_FOUND));
	}

	private JsonNode parse(final String json) {
		return json == null ? null : jsonMapper.readTree(json);
	}
}
