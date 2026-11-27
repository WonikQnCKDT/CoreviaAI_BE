package org.wonik.aiserver.domain.wbs.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftResult;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftStage;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftSubtask;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftTask;

/**
 * AI가 준 예상 공수를 업무 모듈의 반영 규칙(인일, 소수 첫째 자리)에 맞게 반올림한다. 결과 검사보다 먼저 쓴다.
 * 원본은 바꾸지 않고 새 결과를 돌려준다.
 * AI 답(글자) → 읽기(Parser) → 공수 반올림(Normalizer) → 검사(Validator) → 완성
 * [why 해당 코드를 작성하였는가]
 * AI가 공수 3.25를 줌
 *→ 읽기(Parser): 통과
 *→ 검사(Validator): "0.1 이상이네" 하고 통과 (자릿수는 안 봐요)
 *→ 사용자에게 초안이 보임 (정상으로 보여요)
 *→ PM이 [반영] 버튼을 누름
 *→ 백엔드가 "소수 첫째 자리까지만 허용"이라며 거절할 수 도 있음
 *  WbsDraftResultNormalizer의 경우 모델 테스트 후에 문제 없을시 삭제 해도 무방
 */
@Component
public class WbsDraftResultNormalizer {

	private static final int SCALE = 1;

	public AiWbsDraftResult normalize(final AiWbsDraftResult result) {
		if (result == null) {
			return null;
		}
		List<AiWbsDraftStage> stages = result.stages() == null
			? null
			: result.stages().stream().map(this::normalizeStage).toList();
		return new AiWbsDraftResult(
			result.projectId(),
			stages,
			result.milestones(),
			result.feasibility(),
			result.warnings(),
			result.suggestions(),
			result.assumptions(),
			result.clarificationQuestions());
	}

	private AiWbsDraftStage normalizeStage(final AiWbsDraftStage stage) {
		if (stage == null || stage.tasks() == null) {
			return stage;
		}
		return new AiWbsDraftStage(stage.phaseId(), stage.tasks().stream().map(this::normalizeTask).toList());
	}

	private AiWbsDraftTask normalizeTask(final AiWbsDraftTask task) {
		if (task == null) {
			return null;
		}
		List<AiWbsDraftSubtask> subtasks = task.subtasks() == null
			? null
			: task.subtasks().stream().map(this::normalizeSubtask).toList();
		return new AiWbsDraftTask(
			task.tempId(),
			task.name(),
			task.description(),
			round(task.estimatedPersonDays()),
			task.suggestedAssigneeId(),
			task.suggestedRole(),
			task.startDate(),
			task.endDate(),
			task.dependencies(),
			subtasks,
			task.reason());
	}

	private AiWbsDraftSubtask normalizeSubtask(final AiWbsDraftSubtask subtask) {
		if (subtask == null) {
			return null;
		}
		return new AiWbsDraftSubtask(
			subtask.tempId(),
			subtask.name(),
			subtask.description(),
			round(subtask.estimatedPersonDays()));
	}

	private static BigDecimal round(final BigDecimal personDays) {
		return personDays == null ? null : personDays.setScale(SCALE, RoundingMode.HALF_UP);
	}
}