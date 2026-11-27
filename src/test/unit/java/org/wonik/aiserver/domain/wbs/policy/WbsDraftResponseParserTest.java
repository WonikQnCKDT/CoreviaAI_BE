package org.wonik.aiserver.domain.wbs.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftResult;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftTask;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import tools.jackson.databind.json.JsonMapper;

class WbsDraftResponseParserTest {

	private static final String FULL_JSON = """
		{"stages":[{"phaseId":1,"tasks":[
		  {"tempId":"T-001","name":"요구사항 정리","description":"설명","estimatedPersonDays":3.5,
		   "suggestedAssigneeId":11,"suggestedRole":"DEVELOPER","startDate":"2026-12-01","endDate":"2026-12-05",
		   "dependencies":[],"subtasks":[{"tempId":"ST-001","name":"목록 작성","description":"d","estimatedPersonDays":1.5}],
		   "reason":"범위 확정"},
		  {"tempId":"T-002","name":"설계","description":null,"estimatedPersonDays":5,
		   "suggestedAssigneeId":null,"suggestedRole":null,"startDate":"2026-12-08","endDate":"2026-12-17",
		   "dependencies":["T-001"],"subtasks":[],"reason":"요구사항 기반"}
		]}],
		"milestones":[{"tempId":"M-001","name":"설계 완료","targetDate":"2026-12-17","relatedTaskIds":["T-002"],"reason":"확인 시점"}],
		"feasibility":{"status":"AT_RISK","reason":"빠듯함"},
		"warnings":["w"],"suggestions":["s"],"assumptions":["a"],"clarificationQuestions":["q"]}
		""";

	private final WbsDraftResponseParser parser = new WbsDraftResponseParser(JsonMapper.builder().build());

	@Test
	@DisplayName("정상 JSON을 결과 표로 바꾼다")
	void parsesFullResponse() {
		AiWbsDraftResult result = parser.parse(7L, FULL_JSON);

		assertThat(result.projectId()).isEqualTo(7L);
		assertThat(result.stages()).hasSize(1);
		assertThat(result.stages().get(0).phaseId()).isEqualTo(1L);

		AiWbsDraftTask first = result.stages().get(0).tasks().get(0);
		assertThat(first.tempId()).isEqualTo("T-001");
		assertThat(first.name()).isEqualTo("요구사항 정리");
		assertThat(first.estimatedPersonDays()).isEqualByComparingTo("3.5");
		assertThat(first.suggestedAssigneeId()).isEqualTo(11L);
		assertThat(first.suggestedRole()).isEqualTo("DEVELOPER");
		assertThat(first.startDate()).isEqualTo(LocalDate.of(2026, 12, 1));
		assertThat(first.endDate()).isEqualTo(LocalDate.of(2026, 12, 5));
		assertThat(first.dependencies()).isEmpty();
		assertThat(first.subtasks()).hasSize(1);
		assertThat(first.subtasks().get(0).tempId()).isEqualTo("ST-001");
		assertThat(first.subtasks().get(0).estimatedPersonDays()).isEqualByComparingTo("1.5");

		AiWbsDraftTask second = result.stages().get(0).tasks().get(1);
		assertThat(second.description()).isNull();
		assertThat(second.suggestedAssigneeId()).isNull();
		assertThat(second.suggestedRole()).isNull();
		assertThat(second.dependencies()).containsExactly("T-001");
		assertThat(second.estimatedPersonDays()).isEqualByComparingTo("5");

		assertThat(result.milestones()).hasSize(1);
		assertThat(result.milestones().get(0).targetDate()).isEqualTo(LocalDate.of(2026, 12, 17));
		assertThat(result.milestones().get(0).relatedTaskIds()).containsExactly("T-002");
		assertThat(result.feasibility().status()).isEqualTo("AT_RISK");
		assertThat(result.feasibility().reason()).isEqualTo("빠듯함");
		assertThat(result.warnings()).containsExactly("w");
		assertThat(result.suggestions()).containsExactly("s");
		assertThat(result.assumptions()).containsExactly("a");
		assertThat(result.clarificationQuestions()).containsExactly("q");
	}

	@Test
	@DisplayName("JSON 앞뒤에 설명이나 코드블록 표시가 붙어 있어도 읽는다")
	void ignoresTextAroundJson() {
		String text = "다음은 결과입니다.\n```json\n" + FULL_JSON + "\n```\n이상입니다.";

		AiWbsDraftResult result = parser.parse(7L, text);

		assertThat(result.stages().get(0).tasks()).hasSize(2);
	}

	@Test
	@DisplayName("빠진 목록은 빈 목록, 빠진 판단은 null이다")
	void missingOptionalParts() {
		AiWbsDraftResult result = parser.parse(7L, "{\"stages\":[]}");

		assertThat(result.stages()).isEmpty();
		assertThat(result.milestones()).isEmpty();
		assertThat(result.feasibility()).isNull();
		assertThat(result.warnings()).isEmpty();
		assertThat(result.suggestions()).isEmpty();
		assertThat(result.assumptions()).isEmpty();
		assertThat(result.clarificationQuestions()).isEmpty();
	}

	@Test
	@DisplayName("숫자 모양 문자열과 11.0 같은 값도 받는다")
	void lenientNumbers() {
		String json = "{\"stages\":[{\"phaseId\":\"2\",\"tasks\":[{\"tempId\":\"T-001\",\"name\":\"n\","
			+ "\"estimatedPersonDays\":\"2.5\",\"suggestedAssigneeId\":11.0,"
			+ "\"startDate\":\"2026-12-01\",\"endDate\":\"2026-12-02\"}]}]}";

		AiWbsDraftResult result = parser.parse(1L, json);

		assertThat(result.stages().get(0).phaseId()).isEqualTo(2L);
		AiWbsDraftTask task = result.stages().get(0).tasks().get(0);
		assertThat(task.estimatedPersonDays()).isEqualByComparingTo("2.5");
		assertThat(task.suggestedAssigneeId()).isEqualTo(11L);
		assertThat(task.dependencies()).isEmpty();
		assertThat(task.subtasks()).isEmpty();
	}

	@ParameterizedTest(name = "형식이 틀린 답: {0}")
	@NullAndEmptySource
	@ValueSource(strings = {
		"   ",
		"JSON이 없는 그냥 글",
		"{깨진 JSON",
		"[]",
		"{\"milestones\":[]}",
		"{\"stages\":{}}",
		"{\"stages\":[\"문자열\"]}",
		"{\"stages\":[{\"phaseId\":\"abc\",\"tasks\":[]}]}",
		"{\"stages\":[{\"phaseId\":1.5,\"tasks\":[]}]}",
		"{\"stages\":[{\"phaseId\":1,\"tasks\":{}}]}",
		"{\"stages\":[{\"phaseId\":1,\"tasks\":[{\"tempId\":\"T-001\",\"startDate\":\"2026/12/01\"}]}]}",
		"{\"stages\":[{\"phaseId\":1,\"tasks\":[{\"tempId\":\"T-001\",\"estimatedPersonDays\":\"많이\"}]}]}",
		"{\"stages\":[{\"phaseId\":1,\"tasks\":[{\"tempId\":\"T-001\",\"dependencies\":\"T-000\"}]}]}",
		"{\"stages\":[{\"phaseId\":1,\"tasks\":[{\"tempId\":\"T-001\",\"dependencies\":[1]}]}]}",
		"{\"stages\":[{\"phaseId\":1,\"tasks\":[{\"tempId\":\"T-001\",\"name\":{\"a\":1}}]}]}",
		"{\"stages\":[],\"feasibility\":\"FEASIBLE\"}",
		"{\"stages\":[],\"milestones\":[{\"tempId\":\"M-001\",\"targetDate\":\"내일\"}]}"
	})
	@DisplayName("형식이 틀린 답은 502 AI_GENERATION_FAILED로 처리한다")
	void malformedResponses(final String text) {
		assertThatThrownBy(() -> parser.parse(1L, text))
			.isInstanceOfSatisfying(CustomException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_GENERATION_FAILED));
	}

	@Test
	@DisplayName("오류 메시지와 원인에 LLM 답 원문이 남지 않는다")
	void errorDoesNotLeakResponseText() {
		String secret = "SECRET-ORIGINAL-TEXT-123";
		String text = "{\"stages\":[{\"phaseId\":1,\"tasks\":[{\"tempId\":\"" + secret + "\",\"startDate\":\"" + secret + "\"}]}]}";

		assertThatThrownBy(() -> parser.parse(1L, text))
			.isInstanceOfSatisfying(CustomException.class, e -> {
				assertThat(e.getMessage()).doesNotContain(secret);
				assertThat(e.getCause()).isNull();
			});

		assertThatThrownBy(() -> parser.parse(1L, "{" + secret + "}"))
			.isInstanceOfSatisfying(CustomException.class, e -> {
				assertThat(e.getMessage()).doesNotContain(secret);
				assertThat(e.getCause()).isNull();
			});
	}
}
