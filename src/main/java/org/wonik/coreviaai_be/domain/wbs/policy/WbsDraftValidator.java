package org.wonik.coreviaai_be.domain.wbs.policy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.wonik.coreviaai_be.domain.wbs.service.command.ApplyWbsDraftCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftMilestoneCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftSubtaskCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftTaskCommand;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * AI 초안 반영 전 서버 검증(FR-PRJ-07). 초안을 저장하지 않으므로 FE가 보낸 승인 항목을 그대로 믿지 않고
 * WBS 규칙으로 다시 확인한다. 하나라도 어긋나면 전체를 거부한다(일부만 반영하지 않음).
 * <ul>
 *   <li>tempId: 작업·하위작업·마일스톤 전체에서 중복 금지</li>
 *   <li>기간: 시작일 ≤ 종료일, 하위작업은 상위 작업 기간 안. 작업·마일스톤은 프로젝트 기간이 있으면 그 안
 *       (기간은 선택 입력이라 시작일·종료일 중 있는 쪽만 확인한다, FR-PRJ-01)</li>
 *   <li>참조: 선후행·마일스톤 연결은 같은 요청 안의 작업(2레벨)만. 자기 자신을 선행으로 둘 수 없음</li>
 *   <li>순환: 선후행 관계에 순환 금지(FR-WBS-03)</li>
 *   <li>담당자: 프로젝트 멤버만</li>
 * </ul>
 * 이미 등록된 작업을 선행으로 참조하는 방법은 AI API 명세에서 후속으로 남겨 두어 지원하지 않는다.
 */
@Component
public class WbsDraftValidator {

	public void validate(
		final ApplyWbsDraftCommand command,
		final LocalDate projectStart,
		final LocalDate projectEnd,
		final Set<Long> memberUserIds
	) {
		validateUniqueTempIds(command);
		final Set<String> taskTempIds = new HashSet<>();
		for (final DraftTaskCommand task : command.tasks()) {
			validateTask(task, projectStart, projectEnd, memberUserIds);
			taskTempIds.add(task.tempId());
		}
		for (final DraftTaskCommand task : command.tasks()) {
			for (final String predecessor : task.dependencies()) {
				if (!taskTempIds.contains(predecessor) || predecessor.equals(task.tempId())) {
					throw new CustomException(ErrorCode.INVALID_WBS_DRAFT_REFERENCE);
				}
			}
		}
		for (final DraftMilestoneCommand milestone : command.milestones()) {
			requireWithin(milestone.targetDate(), milestone.targetDate(), projectStart, projectEnd);
			if (!taskTempIds.containsAll(milestone.relatedTaskIds())) {
				throw new CustomException(ErrorCode.INVALID_WBS_DRAFT_REFERENCE);
			}
		}
		requireNoCycle(command.tasks());
	}

	private static void validateUniqueTempIds(final ApplyWbsDraftCommand command) {
		final List<String> tempIds = new ArrayList<>();
		command.tasks().forEach(task -> {
			tempIds.add(task.tempId());
			task.subtasks().forEach(subtask -> tempIds.add(subtask.tempId()));
		});
		command.milestones().forEach(milestone -> tempIds.add(milestone.tempId()));
		if (new HashSet<>(tempIds).size() != tempIds.size()) {
			throw new CustomException(ErrorCode.DUPLICATE_WBS_DRAFT_ITEM);
		}
	}

	private static void validateTask(
		final DraftTaskCommand task,
		final LocalDate projectStart,
		final LocalDate projectEnd,
		final Set<Long> memberUserIds
	) {
		requireWithin(task.startDate(), task.endDate(), projectStart, projectEnd);
		requireMember(task.assigneeUserId(), memberUserIds);
		for (final DraftSubtaskCommand subtask : task.subtasks()) {
			requireWithin(subtask.startDateOr(task.startDate()), subtask.endDateOr(task.endDate()),
				task.startDate(), task.endDate());
			requireMember(subtask.assigneeUserId(), memberUserIds);
		}
	}

	/** start ≤ end이고 [rangeStart, rangeEnd] 안인지. 범위 끝이 null이면 그쪽은 제한하지 않는다. */
	private static void requireWithin(
		final LocalDate start,
		final LocalDate end,
		final LocalDate rangeStart,
		final LocalDate rangeEnd
	) {
		if (start.isAfter(end)
			|| (rangeStart != null && start.isBefore(rangeStart))
			|| (rangeEnd != null && end.isAfter(rangeEnd))) {
			throw new CustomException(ErrorCode.INVALID_WBS_DRAFT_SCHEDULE);
		}
	}

	private static void requireMember(final Long assigneeUserId, final Set<Long> memberUserIds) {
		if (assigneeUserId != null && !memberUserIds.contains(assigneeUserId)) {
			throw new CustomException(ErrorCode.INVALID_TASK_ASSIGNEE);
		}
	}

	/** 선행 → 후행 그래프에서 깊이 우선 탐색 중 방문 중인 노드를 다시 만나면 순환이다. */
	private static void requireNoCycle(final List<DraftTaskCommand> tasks) {
		final Map<String, List<String>> predecessorsOf = new HashMap<>();
		tasks.forEach(task -> predecessorsOf.put(task.tempId(), task.dependencies()));
		final Set<String> done = new HashSet<>();
		final Set<String> visiting = new HashSet<>();
		for (final String tempId : predecessorsOf.keySet()) {
			visit(tempId, predecessorsOf, visiting, done);
		}
	}

	private static void visit(
		final String tempId,
		final Map<String, List<String>> predecessorsOf,
		final Set<String> visiting,
		final Set<String> done
	) {
		if (done.contains(tempId)) {
			return;
		}
		if (!visiting.add(tempId)) {
			throw new CustomException(ErrorCode.WBS_DEPENDENCY_CYCLE);
		}
		for (final String predecessor : predecessorsOf.get(tempId)) {
			visit(predecessor, predecessorsOf, visiting, done);
		}
		visiting.remove(tempId);
		done.add(tempId);
	}
}
