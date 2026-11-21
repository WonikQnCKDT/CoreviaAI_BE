package org.wonik.aiserver.domain.wbs.service;

import org.springframework.stereotype.Service;
import org.wonik.aiserver.domain.wbs.api.AiWbsDraftResult;
import org.wonik.aiserver.domain.wbs.api.AiWbsGenerateRequest;
import org.wonik.aiserver.domain.wbs.api.WbsDraftGenerator;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * FastAPI(CoreviaAI_AI)의 /generate/wbs를 Java로 옮기기 전까지 쓰는 임시 구현. 항상 503 {@code AI_NOT_READY}로 응답한다.
 * 이식할 때 프롬프트·LLM 호출·결과 검증을 담은 구현으로 이 클래스를 교체한다.
 */
@Service
public class NotReadyWbsDraftGenerator implements WbsDraftGenerator {

	@Override
	public AiWbsDraftResult generate(final AiWbsGenerateRequest request) {
		throw new CustomException(ErrorCode.AI_NOT_READY);
	}
}
