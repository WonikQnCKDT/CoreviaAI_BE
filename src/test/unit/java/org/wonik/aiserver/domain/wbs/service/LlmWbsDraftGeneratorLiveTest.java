package org.wonik.aiserver.domain.wbs.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftResult;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftStage;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftTask;
import org.wonik.aiserver.domain.wbs.api.AiWbsGenerateRequest;
import org.wonik.aiserver.domain.wbs.api.AiWbsProjectContext;
import org.wonik.aiserver.domain.wbs.api.AiWbsStage;
import org.wonik.aiserver.domain.wbs.api.AiWbsTeamMember;
import org.wonik.aiserver.domain.wbs.policy.WbsDraftResponseParser;
import org.wonik.aiserver.domain.wbs.policy.WbsDraftResultNormalizer;
import org.wonik.aiserver.domain.wbs.policy.WbsDraftResultValidator;
import org.wonik.aiserver.llm.AiLlmProperties;
import org.wonik.aiserver.llm.LlmClient;
import org.wonik.aiserver.llm.WixAiLlmClient;
import org.wonik.global.common.exception.CustomException;

import tools.jackson.databind.json.JsonMapper;

/**
 * 실제 LLM을 부르는 수동 확인용 테스트. AI_LLM_API_KEY가 없으면 건너뛴다.
 * 필요한 환경변수: AI_LLM_BASE_URL, AI_LLM_MODEL, AI_LLM_API_KEY. 키는 화면에 찍지 않는다.
 */
@EnabledIfEnvironmentVariable(named = "AI_LLM_API_KEY", matches = ".+")
class LlmWbsDraftGeneratorLiveTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final Clock clock = Clock.system(ZoneId.of("Asia/Seoul"));

	@Test
	@DisplayName("[수동] 실제 LLM에 짧은 질문을 보내 연결을 확인한다")
	void connection() {
		LlmClient client = realClient();

		long start = System.nanoTime();
		String answer = client.chat("당신은 짧게 답하는 도우미입니다.", "안녕이라고만 답해 주세요.");
		long millis = (System.nanoTime() - start) / 1_000_000;

		System.out.println("[LIVE] 응답 시간(ms) = " + millis);
		System.out.println("[LIVE] 답 = " + answer);
		assertThat(answer).isNotBlank();
	}

	@Test
	@DisplayName("[수동] 실제 LLM으로 작은 프로젝트의 WBS 초안을 만든다")
	void generatesWbsDraft() {
		LlmWbsDraftGenerator generator = new LlmWbsDraftGenerator(
			new WbsPromptBuilder(jsonMapper, clock),
			realClient(),
			new WbsDraftResponseParser(jsonMapper),
			new WbsDraftResultNormalizer(),
			new WbsDraftResultValidator());

		long start = System.nanoTime();
		AiWbsDraftResult result;
		try {
			result = generator.generate(sampleRequest());
		} catch (CustomException e) {
			System.out.println("[LIVE] 실패 오류코드 = " + e.getErrorCode());
			throw e;
		}
		long millis = (System.nanoTime() - start) / 1_000_000;

		System.out.println("[LIVE] 초안 생성 시간(ms) = " + millis);
		for (AiWbsDraftStage stage : result.stages()) {
			for (AiWbsDraftTask task : stage.tasks()) {
				System.out.println("[LIVE] 단계 " + stage.phaseId() + " | " + task.tempId() + " | " + task.name()
					+ " | 공수 " + task.estimatedPersonDays() + " | " + task.startDate() + " ~ " + task.endDate()
					+ " | 선행 " + task.dependencies());
			}
		}
		System.out.println("[LIVE] 마일스톤 수 = " + result.milestones().size());
		System.out.println("[LIVE] 실현 가능성 = " + result.feasibility());
		System.out.println("[LIVE] 경고 = " + result.warnings());
		assertThat(result.stages()).isNotEmpty();
	}

	private LlmClient realClient() {
		String baseUrl = System.getenv("AI_LLM_BASE_URL");
		String model = System.getenv("AI_LLM_MODEL");
		String apiKey = System.getenv("AI_LLM_API_KEY");
		assumeTrue(baseUrl != null && !baseUrl.isBlank(), "AI_LLM_BASE_URL이 없다");
		assumeTrue(model != null && !model.isBlank(), "AI_LLM_MODEL이 없다");

		AiLlmProperties properties = new AiLlmProperties(baseUrl, model, apiKey, Duration.ofSeconds(5),
			Duration.ofSeconds(120), Duration.ofSeconds(180), 2, Duration.ofMillis(500), Duration.ofSeconds(4));
		return new WixAiLlmClient(properties, jsonMapper, clock);
	}

	private static AiWbsGenerateRequest sampleRequest() {
		return new AiWbsGenerateRequest(
			1L,
			"사내 문서 검색 시스템",
			"흩어져 있는 사내 문서를 한 곳에서 검색할 수 있게 한다.",
			"문서 업로드, 키워드 검색, 접근 권한 관리, 검색 결과 하이라이트가 필요하다.",
			new AiWbsProjectContext(LocalDate.of(2026, 12, 1), LocalDate.of(2027, 2, 26), "WATERFALL",
				List.of("개발 인원은 3명")),
			List.of(new AiWbsStage(1L, "기획", 1), new AiWbsStage(2L, "설계", 2), new AiWbsStage(3L, "개발", 3),
				new AiWbsStage(4L, "테스트", 4)),
			List.of(),
			List.of(new AiWbsTeamMember(11L, "김PM", "PM"), new AiWbsTeamMember(12L, "이개발", "DEVELOPER"),
				new AiWbsTeamMember(13L, "박QA", "QA")),
			"작업은 전체 8개 이내로 간단히 만들어 주세요.");
	}
}
