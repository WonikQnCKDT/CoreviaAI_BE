package org.wonik.coreviaai_be.domain.worklog.service;

import java.time.LocalDate;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.project.policy.ProjectAccessPolicy;
import org.wonik.coreviaai_be.domain.wbs.policy.WbsTaskAccessPolicy;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.service.TaskDetailService;
import org.wonik.coreviaai_be.domain.wbs.service.command.UpdateTaskProgressCommand;
import org.wonik.coreviaai_be.domain.worklog.entity.Worklog;
import org.wonik.coreviaai_be.domain.worklog.entity.WorklogTask;
import org.wonik.coreviaai_be.domain.worklog.repository.WorklogRepository;
import org.wonik.coreviaai_be.domain.worklog.repository.WorklogTaskRepository;
import org.wonik.coreviaai_be.domain.worklog.service.command.WorklogTaskCommand;
import org.wonik.coreviaai_be.domain.worklog.service.result.WorklogTaskPage;
import org.wonik.coreviaai_be.domain.worklog.service.result.WorklogTaskResult;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.time.QueryRangePolicy;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorklogTaskService {
    private final WorklogTaskRepository entries;
    private final WorklogRepository logs;
    private final WbsTaskRepository tasks;
    private final WbsTaskAccessPolicy access;
    private final ProjectAccessPolicy projects;
    private final TaskDetailService taskDetails;
    private final EntityManager em;

    @Transactional
    public WorklogTaskResult upsert(AuditActor actor, LocalDate date, Long taskId, WorklogTaskCommand command) {
        if (projects.getViewer(actor.userId()).getSystemRole() != SystemRole.USER) {
            throw new CustomException(ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
        }
        final var editor = access.getEditor(actor.userId(), command.projectId(), WbsTaskAccessPolicy.TASK_FEATURES);
        final var task = tasks.findByIdAndProjectId(taskId, command.projectId())
            .orElseThrow(() -> new CustomException(ErrorCode.WBS_TASK_NOT_FOUND));
        editor.requireCanUpdate(task);
        if (!task.isAssignedTo(actor.userId())) throw new CustomException(ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
        if (command.completed() && command.progress() != 100) {
            throw new CustomException(ErrorCode.WBS_COMPLETED_PROGRESS_REQUIRED);
        }
        if (!command.completed() && task.getStatus() == org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus.DONE) {
            throw new CustomException(ErrorCode.WBS_COMPLETED_PROGRESS_REQUIRED);
        }
        // Project -> user is the fixed lock order. A single request changes only one project.
        try {
            if (em.find(User.class, actor.userId(), LockModeType.PESSIMISTIC_WRITE) == null) {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }
        } catch (jakarta.persistence.PessimisticLockException | jakarta.persistence.LockTimeoutException ex) {
            throw new CustomException(ErrorCode.CONCURRENT_MODIFICATION, ex);
        }
        final var log = logs.findByUserIdAndLogDate(actor.userId(), date)
            .orElseGet(() -> logs.save(Worklog.create(actor.userId(), date, null)));
        final var existing = entries.findByWorklogIdAndTaskId(log.getId(), taskId);
        if (existing.isPresent() && existing.get().matches(command.content(), command.progress(),
            command.spentMinutes(), command.completed())) {
            return WorklogTaskResult.from(existing.get(), actor.userId(), date);
        }
        if (existing.map(WorklogTask::getVersion).orElse(0L) != command.version()) {
            throw new CustomException(ErrorCode.CONCURRENT_MODIFICATION);
        }
        if (command.completed()) {
            taskDetails.complete(actor, command.projectId(), taskId, command.taskVersion());
        } else {
            taskDetails.updateProgress(actor, command.projectId(), taskId,
                new UpdateTaskProgressCommand(command.progress(), command.taskVersion()));
        }
        final var entry = existing.orElseGet(() -> WorklogTask.create(log.getId(), taskId));
        entry.replace(command.content(), command.progress(), command.spentMinutes(), command.completed());
        entries.saveAndFlush(entry);
        return WorklogTaskResult.from(entry, actor.userId(), date);
    }

    public WorklogTaskPage findOwned(Long userId, LocalDate date, int page, int size) {
        return results(entries.findOwned(userId, date, PageRequest.of(page, size)));
    }

    public WorklogTaskPage findProject(Long viewerId, Long projectId, LocalDate from, LocalDate to, int page, int size) {
        if (projects.getViewer(viewerId).getSystemRole() != SystemRole.USER) {
            throw new CustomException(ErrorCode.PROJECT_PM_ONLY);
        }
        projects.getManagedProject(viewerId, projectId);
        QueryRangePolicy.requireDateRange(from, to);
        return results(entries.findProject(projectId, from, to, PageRequest.of(page, size)));
    }

    private WorklogTaskPage results(Slice<WorklogTask> page) {
        // Batch metadata lookup; never serialize the private daily content to a PM.
        final var notes = logs.findAllById(page.stream().map(WorklogTask::getWorklogId).distinct().toList())
            .stream().collect(Collectors.toMap(Worklog::getId, Function.identity()));
        return new WorklogTaskPage(page.stream().map(e -> WorklogTaskResult.from(e,
            notes.get(e.getWorklogId()).getUserId(), notes.get(e.getWorklogId()).getLogDate())).toList(), page.hasNext());
    }
}
