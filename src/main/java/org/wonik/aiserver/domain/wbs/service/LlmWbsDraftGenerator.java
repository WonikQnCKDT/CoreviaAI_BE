package org.wonik.aiserver.domain.wbs.service;

import org.springframework.stereotype.Service;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftResult;
import org.wonik.aiserver.domain.wbs.api.AiWbsGenerateRequest;
import org.wonik.aiserver.domain.wbs.api.WbsDraftGenerator;
import org.wonik.aiserver.domain.wbs.policy.WbsDraftResponseParser;
import org.wonik.aiserver.domain.wbs.policy.WbsDraftResultNormalizer;
import org.wonik.aiserver.domain.wbs.policy.WbsDraftResultValidator;
import org.wonik.aiserver.llm.LlmClient;

/**
 * LLM으로 WBS 초안을 만든다. 질문 만들기 → LLM 호출 → 답 읽기 → 공수 맞추기 → 결과 검사 순서로 진행한다.
 * LLM 호출 오류(503/504)와 답 오류(502)는 각 부품이 던지는 CustomException을 그대로 전달한다.
 * 목적·요구사항이 모두 비어 있으면 질문을 만들 수 없어 IllegalArgumentException이 난다(업무 모듈이 먼저 걸러야 한다).
 */
@Service
public class LlmWbsDraftGenerator implements WbsDraftGenerator {

	private final WbsPromptBuilder promptBuilder;
	private final LlmClient llmClient;
	private final WbsDraftResponseParser responseParser;
	private final WbsDraftResultNormalizer resultNormalizer;
	private final WbsDraftResultValidator resultValidator;

	public LlmWbsDraftGenerator(
		final WbsPromptBuilder promptBuilder,
		final LlmClient llmClient,
		final WbsDraftResponseParser responseParser,
		final WbsDraftResultNormalizer resultNormalizer,
		final WbsDraftResultValidator resultValidator) {
		this.promptBuilder = promptBuilder;
		this.llmClient = llmClient;
		this.responseParser = responseParser;
		this.resultNormalizer = resultNormalizer;
		this.resultValidator = resultValidator;
	}

	@Override
	public AiWbsDraftResult generate(final AiWbsGenerateRequest request) {
		String systemPrompt = promptBuilder.systemPrompt();
		String userPrompt = promptBuilder.userPrompt(request);

		String answer = llmClient.chat(systemPrompt, userPrompt);

		AiWbsDraftResult parsed = responseParser.parse(request.projectId(), answer);
		AiWbsDraftResult normalized = resultNormalizer.normalize(parsed);
		resultValidator.validate(request, normalized);
		return normalized;
	}
}