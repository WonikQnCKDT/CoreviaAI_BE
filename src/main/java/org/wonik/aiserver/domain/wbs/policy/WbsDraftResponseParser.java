package org.wonik.aiserver.domain.wbs.policy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftMilestone;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftResult;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftStage;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftSubtask;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftTask;
import org.wonik.aiserver.domain.wbs.api.AiWbsFeasibility;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * LLM이 돌려준 글에서 JSON을 꺼내 {@link AiWbsDraftResult}로 바꾼다. 모양(형식)만 확인하고, tempId 중복·순환·기간
 * 같은 내용 검증은 하지 않는다. 형식이 틀리면 502 {@code AI_GENERATION_FAILED}다.
 * <p>
 * LLM 답 원문이 오류 메시지나 로그에 남지 않도록, 원인 예외(JSON 오류 메시지에 원문 일부가 들어간다)는 붙이지 않는다.
 * 답 읽기. 해당코드는 LLM이 준 글(JSON)을 우리 결과 표로 옮겨 적는 용도. 모양이 깨졌는지만 확인.
 * LLM의 글 → [답 읽기: Parser] → 결과 표 → [결과 검사: Validator] → 통과하면 백엔드로
 */
@Component
public class WbsDraftResponseParser {

	private final JsonMapper jsonMapper;

	public WbsDraftResponseParser(final JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	/**
	 * @param projectId 요청에서 받은 프로젝트 ID(LLM이 정하지 않는다)
	 * @param llmText   LLM이 돌려준 글. JSON 앞뒤에 설명이나 코드블록 표시가 붙어 있어도 된다
	 */
	public AiWbsDraftResult parse(final Long projectId, final String llmText) {
		JsonNode root = readRoot(llmText);

		JsonNode stagesNode = root.get("stages");
		if (stagesNode == null || !stagesNode.isArray()) {
			throw failed();
		}
		List<AiWbsDraftStage> stages = new ArrayList<>();
		for (JsonNode stageNode : stagesNode) {
			stages.add(toStage(stageNode));
		}

		List<AiWbsDraftMilestone> milestones = new ArrayList<>();
		for (JsonNode milestoneNode : arrayOrEmpty(root.get("milestones"))) {
			milestones.add(toMilestone(milestoneNode));
		}

		return new AiWbsDraftResult(
			projectId,
			stages,
			milestones,
			toFeasibility(root.get("feasibility")),
			stringList(root.get("warnings")),
			stringList(root.get("suggestions")),
			stringList(root.get("assumptions")),
			stringList(root.get("clarificationQuestions")));
	}

	/** 첫 '{'부터 마지막 '}'까지만 잘라 읽는다. 앞뒤 설명이나 ```json 표시는 버린다. */
	private JsonNode readRoot(final String llmText) {
		if (llmText == null || llmText.isBlank()) {
			throw failed();
		}
		int start = llmText.indexOf('{');
		int end = llmText.lastIndexOf('}');
		if (start < 0 || end < start) {
			throw failed();
		}
		JsonNode root;
		try {
			root = jsonMapper.readTree(llmText.substring(start, end + 1));
		} catch (JacksonException e) {
			throw failed();
		}
		if (root == null || !root.isObject()) {
			throw failed();
		}
		return root;
	}

	private AiWbsDraftStage toStage(final JsonNode node) {
		requireObject(node);
		List<AiWbsDraftTask> tasks = new ArrayList<>();
		for (JsonNode taskNode : arrayOrEmpty(node.get("tasks"))) {
			tasks.add(toTask(taskNode));
		}
		return new AiWbsDraftStage(longValue(node.get("phaseId")), tasks);
	}

	private AiWbsDraftTask toTask(final JsonNode node) {
		requireObject(node);
		List<AiWbsDraftSubtask> subtasks = new ArrayList<>();
		for (JsonNode subtaskNode : arrayOrEmpty(node.get("subtasks"))) {
			subtasks.add(toSubtask(subtaskNode));
		}
		return new AiWbsDraftTask(
			text(node.get("tempId")),
			text(node.get("name")),
			text(node.get("description")),
			decimal(node.get("estimatedPersonDays")),
			longValue(node.get("suggestedAssigneeId")),
			text(node.get("suggestedRole")),
			date(node.get("startDate")),
			date(node.get("endDate")),
			stringList(node.get("dependencies")),
			subtasks,
			text(node.get("reason")));
	}

	private AiWbsDraftSubtask toSubtask(final JsonNode node) {
		requireObject(node);
		return new AiWbsDraftSubtask(
			text(node.get("tempId")),
			text(node.get("name")),
			text(node.get("description")),
			decimal(node.get("estimatedPersonDays")));
	}

	private AiWbsDraftMilestone toMilestone(final JsonNode node) {
		requireObject(node);
		return new AiWbsDraftMilestone(
			text(node.get("tempId")),
			text(node.get("name")),
			date(node.get("targetDate")),
			stringList(node.get("relatedTaskIds")),
			text(node.get("reason")));
	}

	private AiWbsFeasibility toFeasibility(final JsonNode node) {
		if (!isPresent(node)) {
			return null;
		}
		requireObject(node);
		return new AiWbsFeasibility(text(node.get("status")), text(node.get("reason")));
	}

	private static boolean isPresent(final JsonNode node) {
		return node != null && !node.isNull();
	}

	private static void requireObject(final JsonNode node) {
		if (node == null || !node.isObject()) {
			throw failed();
		}
	}

	/** 없거나 null이면 빈 목록처럼 다루고, 있는데 배열이 아니면 형식 오류다. */
	private static Iterable<JsonNode> arrayOrEmpty(final JsonNode node) {
		if (!isPresent(node)) {
			return List.of();
		}
		if (!node.isArray()) {
			throw failed();
		}
		return node;
	}

	private static List<String> stringList(final JsonNode node) {
		List<String> values = new ArrayList<>();
		for (JsonNode item : arrayOrEmpty(node)) {
			if (!item.isString()) {
				throw failed();
			}
			values.add(item.asString());
		}
		return values;
	}

	/** 문자열이거나 null만 받는다. 숫자나 객체가 오면 형식 오류다. */
	private static String text(final JsonNode node) {
		if (!isPresent(node)) {
			return null;
		}
		if (!node.isString()) {
			throw failed();
		}
		return node.asString();
	}

	/** 숫자(3.5)나 숫자 모양 문자열("3.5")을 받는다. */
	private static BigDecimal decimal(final JsonNode node) {
		if (!isPresent(node)) {
			return null;
		}
		if (node.isNumber()) {
			return node.decimalValue();
		}
		if (node.isString()) {
			try {
				return new BigDecimal(node.asString().trim());
			} catch (NumberFormatException e) {
				throw failed();
			}
		}
		throw failed();
	}

	/** 정수(11), 11.0, "11"을 받는다. 11.5처럼 소수가 남는 값은 형식 오류다. */
	private static Long longValue(final JsonNode node) {
		BigDecimal value = decimal(node);
		if (value == null) {
			return null;
		}
		try {
			return value.longValueExact();
		} catch (ArithmeticException e) {
			throw failed();
		}
	}

	/** YYYY-MM-DD 문자열만 받는다. */
	private static LocalDate date(final JsonNode node) {
		if (!isPresent(node)) {
			return null;
		}
		if (!node.isString()) {
			throw failed();
		}
		try {
			return LocalDate.parse(node.asString().trim());
		} catch (DateTimeParseException e) {
			throw failed();
		}
	}

	private static CustomException failed() {
		return new CustomException(ErrorCode.AI_GENERATION_FAILED);
	}
}
