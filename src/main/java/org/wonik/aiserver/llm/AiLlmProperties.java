package org.wonik.aiserver.llm;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 사내 LLM(OpenAI 호환 API) 호출 설정(app.ai.llm.*).
 *
 * @param baseUrl           LLM 서버 주소(503)
 * @param model             사용할 모델 이름(503)
 * @param apiKey            LLM 서버 인증 키. 환경변수로만 주입
 * @param connectTimeout    서버에 연결하기까지 기다리는 시간
 * @param readTimeout       한 번의 요청에서 응답을 기다리는 시간. 긴 명세를 처리해 1분을 넘길 수 있어 길게 둔다
 * @param overallDeadline   재시도를 모두 합친 전체 제한 시간(504)
 * @param maxRetries        일시 오류일 때 다시 시도하는 최대 횟수(처음 시도는 제외)
 * @param retryInitialDelay 첫 재시도 전 대기 시간. 이후 두 배씩 늘리기 
 * @param retryMaxDelay     재시도 대기 시간의 상한
 */
@ConfigurationProperties(prefix = "app.ai.llm")
public record AiLlmProperties(
	String baseUrl,
	String model,
	String apiKey,
	@DefaultValue("5s") Duration connectTimeout,
	@DefaultValue("120s") Duration readTimeout,
	@DefaultValue("180s") Duration overallDeadline,
	@DefaultValue("2") int maxRetries,
	@DefaultValue("500ms") Duration retryInitialDelay,
	@DefaultValue("4s") Duration retryMaxDelay) {

	public AiLlmProperties {
		requirePositive(connectTimeout, "connectTimeout");
		requirePositive(readTimeout, "readTimeout");
		requirePositive(overallDeadline, "overallDeadline");
		requirePositive(retryInitialDelay, "retryInitialDelay");
		requirePositive(retryMaxDelay, "retryMaxDelay");
		if (maxRetries < 0) {
			throw new IllegalArgumentException("maxRetries는 0 이상이어야 한다");
		}
		if (retryInitialDelay.compareTo(retryMaxDelay) > 0) {
			throw new IllegalArgumentException("retryInitialDelay는 retryMaxDelay보다 클 수 없다");
		}
	}

	/** 주소와 모델이 모두 있어야 LLM을 호출할 수 있다. */
	public boolean isConfigured() {
		return baseUrl != null && !baseUrl.isBlank() && model != null && !model.isBlank();
	}

	/** 기본 toString이 apiKey를 그대로 찍지 않게 가린다. */
	@Override
	public String toString() {
		return "AiLlmProperties[baseUrl=" + baseUrl + ", model=" + model + ", apiKey=****"
			+ ", connectTimeout=" + connectTimeout + ", readTimeout=" + readTimeout
			+ ", overallDeadline=" + overallDeadline + ", maxRetries=" + maxRetries + "]";
	}

	private static void requirePositive(Duration value, String name) {
		if (value == null || value.isZero() || value.isNegative()) {
			throw new IllegalArgumentException(name + "은 0보다 커야 한다");
		}
	}
}
