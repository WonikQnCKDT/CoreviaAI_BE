package org.wonik.aiserver.domain.wbs.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftMilestone;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftResult;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftStage;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftSubtask;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftTask;
import org.wonik.aiserver.domain.wbs.api.AiWbsFeasibility;
import org.wonik.aiserver.domain.wbs.api.AiWbsGenerateRequest;
import org.wonik.aiserver.domain.wbs.api.AiWbsProjectContext;
import org.wonik.aiserver.domain.wbs.api.AiWbsStage;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * AI가 만든 WBS 초안이 업무 모듈이 받아 줄 수 있는 모양인지 확인한다(04. WBS·일정 API의 반영 규칙 기준).
 * 틀리면 502 {@code AI_GENERATION_FAILED}다. 값을 바꾸지 않고 확인만 한다.
 * <p>
 * 오류 메시지와 로그에는 AI가 쓴 내용을 남기지 않고, 어떤 규칙에서 틀렸는지만 남긴다.
 */
@Component
public class WbsDraftResultValidator {

	private static final Logger log = LoggerFactory.getLogger(WbsDraftResultValidator.class);

	private static final int MAX_TASKS = 200;
	private static final int MAX_SUBTASKS_PER_TASK = 50;
	private static final int MAX_MILESTONES = 50;
	private static final int MAX_TEMP_ID_LENGTH = 50;
	private static final int MAX_NAME_LENGTH = 200;
	private static final int MAX_DESCRIPTION_LENGTH = 2000;
	private static final BigDecimal MIN_PERSON_DAYS = new BigDecimal("0.1");
	private static final Set<String> FEASIBILITY_STATUSES =
		Set.of("FEASIBLE", "AT_RISK", "INFEASIBLE", "INSUFFICIENT_INFO");
	private static final String INSUFFICIENT_INFO = "INSUFFICIENT_INFO";

	private static final int VISITING = 1;
	private static final int DONE = 2;

	/**
	 * @throws CustomException 결과가 규칙에 맞지 않을 때(502 AI_GENERATION_FAILED)
	 */
	public void validate(final AiWbsGenerateRequest request, final AiWbsDraftResult result) {
		if (request == null || result == null || result.stages() == null) {
			throw reject("결과가 비어 있음");
		}

		Set<Long> phaseIds = phaseIds(request);
		AiWbsProjectContext context = request.projectContext();
		LocalDate periodStart = context == null ? null : context.startDate();
		LocalDate periodEnd = context == null ? null : context.endDate();

		Set<String> usedIds = new HashSet<>();
		Map<String, AiWbsDraftTask> tasksById = new LinkedHashMap<>();
		for (AiWbsDraftStage stage : result.stages()) {
			if (stage == null || stage.phaseId() == null || !phaseIds.contains(stage.phaseId())) {
				throw reject("요청에 없는 단계");
			}
			for (AiWbsDraftTask task : orEmpty(stage.tasks())) {
				validateTask(task, usedIds, periodStart, periodEnd);
				tasksById.put(task.tempId(), task);
				if (tasksById.size() > MAX_TASKS) {
					throw reject("작업 개수 초과");
				}
			}
		}

		List<AiWbsDraftMilestone> milestones = orEmpty(result.milestones());
		if (milestones.size() > MAX_MILESTONES) {
			throw reject("마일스톤 개수 초과");
		}
		for (AiWbsDraftMilestone milestone : milestones) {
			validateMilestone(milestone, usedIds, tasksById, periodStart, periodEnd);
		}

		validateDependencies(tasksById);
		validateFeasibility(result.feasibility());

		if (tasksById.isEmpty() && milestones.isEmpty() && !isInsufficientInfo(result.feasibility())) {
			throw reject("빈 초안");
		}
	}

	private static Set<Long> phaseIds(final AiWbsGenerateRequest request) {
		Set<Long> ids = new HashSet<>();
		for (AiWbsStage stage : orEmpty(request.existingStages())) {
			if (stage != null && stage.phaseId() != null) {
				ids.add(stage.phaseId());
			}
		}
		return ids;
	}

	private static void validateTask(final AiWbsDraftTask task, final Set<String> usedIds,
		final LocalDate periodStart, final LocalDate periodEnd) {
		if (task == null) {
			throw reject("작업이 null");
		}
		requireUniqueId(task.tempId(), usedIds);
		requireText(task.name(), MAX_NAME_LENGTH, "작업 이름");
		requireMaxLength(task.description(), MAX_DESCRIPTION_LENGTH, "작업 설명");
		requirePersonDays(task.estimatedPersonDays());
		requireWithinPeriod(task.startDate(), periodStart, periodEnd, "작업 시작일");
		requireWithinPeriod(task.endDate(), periodStart, periodEnd, "작업 종료일");
		if (task.startDate().isAfter(task.endDate())) {
			throw reject("시작일이 종료일보다 늦음");
		}

		List<AiWbsDraftSubtask> subtasks = orEmpty(task.subtasks());
		if (subtasks.size() > MAX_SUBTASKS_PER_TASK) {
			throw reject("하위작업 개수 초과");
		}
		for (AiWbsDraftSubtask subtask : subtasks) {
			if (subtask == null) {
				throw reject("하위작업이 null");
			}
			requireUniqueId(subtask.tempId(), usedIds);
			requireText(subtask.name(), MAX_NAME_LENGTH, "하위작업 이름");
			requireMaxLength(subtask.description(), MAX_DESCRIPTION_LENGTH, "하위작업 설명");
			requirePersonDays(subtask.estimatedPersonDays());
		}
	}

	private static void validateMilestone(final AiWbsDraftMilestone milestone, final Set<String> usedIds,
		final Map<String, AiWbsDraftTask> tasksById, final LocalDate periodStart, final LocalDate periodEnd) {
		if (milestone == null) {
			throw reject("마일스톤이 null");
		}
		requireUniqueId(milestone.tempId(), usedIds);
		requireText(milestone.name(), MAX_NAME_LENGTH, "마일스톤 이름");
		requireWithinPeriod(milestone.targetDate(), periodStart, periodEnd, "마일스톤 날짜");
		for (String taskId : orEmpty(milestone.relatedTaskIds())) {
			if (taskId == null || !tasksById.containsKey(taskId)) {
				throw reject("마일스톤이 없는 작업을 가리킴");
			}
		}
	}

	/** 선행 작업이 실제 작업(하위작업 아님)이고 자기 자신이 아닌지, 순환이 없는지 확인한다. */
	private static void validateDependencies(final Map<String, AiWbsDraftTask> tasksById) {
		for (Map.Entry<String, AiWbsDraftTask> entry : tasksById.entrySet()) {
			for (String dependency : orEmpty(entry.getValue().dependencies())) {
				if (dependency == null || !tasksById.containsKey(dependency)) {
					throw reject("없는 선행 작업");
				}
				if (dependency.equals(entry.getKey())) {
					throw reject("자기 자신을 선행으로 지정");
				}
			}
		}
		Map<String, Integer> state = new HashMap<>();
		for (String taskId : tasksById.keySet()) {
			if (hasCycle(taskId, tasksById, state)) {
				throw reject("선행 작업 순환");
			}
		}
	}

	/** 깊이 우선 탐색. 탐색 중인 작업(VISITING)을 다시 만나면 순환이다. */
	private static boolean hasCycle(final String taskId, final Map<String, AiWbsDraftTask> tasksById,
		final Map<String, Integer> state) {
		Integer current = state.get(taskId);
		if (current != null) {
			return current == VISITING;
		}
		state.put(taskId, VISITING);
		for (String dependency : orEmpty(tasksById.get(taskId).dependencies())) {
			if (hasCycle(dependency, tasksById, state)) {
				return true;
			}
		}
		state.put(taskId, DONE);
		return false;
	}

	private static void validateFeasibility(final AiWbsFeasibility feasibility) {
		if (feasibility == null) {
			return;
		}
		if (feasibility.status() == null || !FEASIBILITY_STATUSES.contains(feasibility.status())) {
			throw reject("판단 상태 값");
		}
	}

	private static boolean isInsufficientInfo(final AiWbsFeasibility feasibility) {
		return feasibility != null && INSUFFICIENT_INFO.equals(feasibility.status());
	}

	private static void requireUniqueId(final String tempId, final Set<String> usedIds) {
		if (tempId == null || tempId.isBlank() || tempId.length() > MAX_TEMP_ID_LENGTH) {
			throw reject("tempId 형식");
		}
		if (!usedIds.add(tempId)) {
			throw reject("tempId 중복");
		}
	}

	private static void requireText(final String value, final int maxLength, final String what) {
		if (value == null || value.isBlank() || value.length() > maxLength) {
			throw reject(what);
		}
	}

	private static void requireMaxLength(final String value, final int maxLength, final String what) {
		if (value != null && value.length() > maxLength) {
			throw reject(what);
		}
	}

	private static void requirePersonDays(final BigDecimal personDays) {
		if (personDays != null && personDays.compareTo(MIN_PERSON_DAYS) < 0) {
			throw reject("공수가 0.1 미만");
		}
	}

	/** 날짜는 반드시 있어야 하고, 프로젝트 기간이 있으면 그 안이어야 한다(시작·종료 중 있는 쪽만 확인). */
	private static void requireWithinPeriod(final LocalDate date, final LocalDate periodStart,
		final LocalDate periodEnd, final String what) {
		if (date == null) {
			throw reject(what + " 없음");
		}
		if (periodStart != null && date.isBefore(periodStart)) {
			throw reject(what + "이 프로젝트 기간 앞");
		}
		if (periodEnd != null && date.isAfter(periodEnd)) {
			throw reject(what + "이 프로젝트 기간 뒤");
		}
	}

	private static <T> List<T> orEmpty(final List<T> list) {
		return list == null ? List.of() : list;
	}

	/** 어떤 규칙에서 틀렸는지만 로그에 남기고, AI가 쓴 내용은 남기지 않는다. */
	private static CustomException reject(final String rule) {
		log.warn("AI WBS 초안 검증 실패: {}", rule);
		return new CustomException(ErrorCode.AI_GENERATION_FAILED);
	}
}