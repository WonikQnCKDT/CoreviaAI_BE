package org.wonik.aiserver.domain.wbs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftResult;
import org.wonik.aiserver.domain.wbs.api.AiWbsGenerateRequest;
import org.wonik.aiserver.domain.wbs.api.AiWbsStage;
import org.wonik.aiserver.domain.wbs.policy.WbsDraftResponseParser;
import org.wonik.aiserver.domain.wbs.policy.WbsDraftResultNormalizer;
import org.wonik.aiserver.domain.wbs.policy.WbsDraftResultValidator;
import org.wonik.aiserver.llm.LlmClient;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import tools.jackson.databind.json.JsonMapper;

class LlmWbsDraftGeneratorTest {

	private static final String VALID_ANSWER = """
		{"stages":[{"phaseId":1,"tasks":[{"tempId":"T-001","name":"로그인 화면","description":"설명",
		"estimatedPersonDays":3.25,"startDate":"2026-12-01","endDate":"2026-12-03",
		"dependencies":[],"subtasks":[],"reason":"이유"}]}],
		"milestones":[],"feasibility":{"status":"FEASIBLE","reason":"ok"}}
		""";

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final Clock clock = Clock.fixed(Instant.parse("2026-11-27T00:00:00Z"), ZoneId.of("Asia/Seoul"));

	@Test
	@DisplayName("정상 답이면 공수가 반올림된 초안을 돌려준다")
	void generatesDraft() {
		LlmWbsDraftGenerator generator = generatorWith((system, user) -> VALID_ANSWER);

		AiWbsDraftResult result = generator.generate(request("로그인 기능을 만든다"));

		assertThat(result.projectId()).isEqualTo(7L);
		assertThat(result.stages()).hasSize(1);
		assertThat(result.stages().get(0).tasks().get(0).estimatedPersonDays()).isEqualTo(new BigDecimal("3.3"));
	}

	@Test
	@DisplayName("AI에게 시스템 지시와 프로젝트 정보가 담긴 질문을 보낸다")
	void sendsPrompts() {
		List<String> sent = new ArrayList<>();
		LlmWbsDraftGenerator generator = generatorWith((system, user) -> {
			sent.add(system);
			sent.add(user);
			return VALID_ANSWER;
		});

		generator.generate(request("로그인 기능을 만든다"));

		assertThat(sent.get(0)).isNotBlank();
		assertThat(sent.get(1)).contains("테스트 프로젝트").contains("로그인 기능을 만든다");
	}

	@Test
	@DisplayName("AI 호출 오류(504 등)는 그대로 전달한다")
	void propagatesLlmError() {
		CustomException timeout = new CustomException(ErrorCode.AI_TIMEOUT);
		LlmWbsDraftGenerator generator = generatorWith((system, user) -> {
			throw timeout;
		});

		assertThatThrownBy(() -> generator.generate(request("로그인 기능을 만든다"))).isSameAs(timeout);
	}

	@Test
	@DisplayName("AI 답이 JSON이 아니면 502(AI_GENERATION_FAILED)")
	void malformedAnswerFails() {
		LlmWbsDraftGenerator generator = generatorWith((system, user) -> "죄송합니다. 만들 수 없습니다.");

		assertThatThrownBy(() -> generator.generate(request("로그인 기능을 만든다")))
			.isInstanceOfSatisfying(CustomException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_GENERATION_FAILED));
	}

	@Test
	@DisplayName("검사에 걸리면(없는 단계 번호) 502(AI_GENERATION_FAILED)")
	void invalidResultFails() {
		String unknownPhase = VALID_ANSWER.replace("\"phaseId\":1", "\"phaseId\":99");
		LlmWbsDraftGenerator generator = generatorWith((system, user) -> unknownPhase);

		assertThatThrownBy(() -> generator.generate(request("로그인 기능을 만든다")))
			.isInstanceOfSatisfying(CustomException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_GENERATION_FAILED));
	}

	@Test
	@DisplayName("목적과 요구사항이 모두 비어 있으면 AI를 부르지 않고 IllegalArgumentException")
	void blankInputIsRejectedBeforeCallingLlm() {
		List<String> calls = new ArrayList<>();
		LlmWbsDraftGenerator generator = generatorWith((system, user) -> {
			calls.add("called");
			return VALID_ANSWER;
		});

		assertThatThrownBy(() -> generator.generate(request(" ")))
			.isInstanceOf(IllegalArgumentException.class);
		assertThat(calls).isEmpty();
	}

	private LlmWbsDraftGenerator generatorWith(final LlmClient llmClient) {
		return new LlmWbsDraftGenerator(
			new WbsPromptBuilder(jsonMapper, clock),
			llmClient,
			new WbsDraftResponseParser(jsonMapper),
			new WbsDraftResultNormalizer(),
			new WbsDraftResultValidator());
	}

	private static AiWbsGenerateRequest request(final String purpose) {
		return new AiWbsGenerateRequest(
			7L,
			"테스트 프로젝트",
			purpose,
			null,
			null,
			List.of(new AiWbsStage(1L, "개발", 1)),
			List.of(),
			List.of(),
			null);
	}
}