package org.wonik.aiserver.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AiLlmPropertiesTest {

	private static final Duration SEC = Duration.ofSeconds(1);

	private static AiLlmProperties endpoint(final String baseUrl, final String model) {
		return new AiLlmProperties(baseUrl, model, "key", SEC, SEC, SEC, 2, Duration.ofMillis(500), SEC);
	}

	private static AiLlmProperties timing(final Duration connect, final Duration read, final Duration overall,
		final int retries, final Duration initialDelay, final Duration maxDelay) {
		return new AiLlmProperties("u", "m", "key", connect, read, overall, retries, initialDelay, maxDelay);
	}

	@Test
	@DisplayName("주소와 모델이 모두 있으면 준비된 상태")
	void configured() {
		assertThat(endpoint("http://llm.example", "model-a").isConfigured()).isTrue();
	}

	@Test
	@DisplayName("주소나 모델이 null·빈 문자열·공백이면 준비되지 않은 상태")
	void notConfigured() {
		assertThat(endpoint(null, "model-a").isConfigured()).isFalse();
		assertThat(endpoint("", "model-a").isConfigured()).isFalse();
		assertThat(endpoint("   ", "model-a").isConfigured()).isFalse();
		assertThat(endpoint("http://llm.example", null).isConfigured()).isFalse();
		assertThat(endpoint("http://llm.example", " ").isConfigured()).isFalse();
	}

	@Test
	@DisplayName("시간 값이 0이거나 음수이거나 null이면 거부")
	void rejectsNonPositiveDurations() {
		assertThatThrownBy(() -> timing(Duration.ZERO, SEC, SEC, 0, SEC, SEC))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> timing(SEC, SEC.negated(), SEC, 0, SEC, SEC))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> timing(SEC, SEC, null, 0, SEC, SEC))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("재시도 횟수가 음수면 거부, 0은 허용")
	void maxRetriesBoundary() {
		assertThatThrownBy(() -> timing(SEC, SEC, SEC, -1, SEC, SEC))
			.isInstanceOf(IllegalArgumentException.class);
		assertThat(timing(SEC, SEC, SEC, 0, SEC, SEC).maxRetries()).isZero();
	}

	@Test
	@DisplayName("첫 재시도 대기가 상한보다 크면 거부, 같으면 허용")
	void retryDelayOrder() {
		assertThatThrownBy(() -> timing(SEC, SEC, SEC, 1, Duration.ofSeconds(6), Duration.ofSeconds(5)))
			.isInstanceOf(IllegalArgumentException.class);
		assertThat(timing(SEC, SEC, SEC, 1, Duration.ofSeconds(5), Duration.ofSeconds(5)).retryMaxDelay())
			.isEqualTo(Duration.ofSeconds(5));
	}

	@Test
	@DisplayName("toString에 API 키가 나오지 않는다")
	void toStringMasksApiKey() {
		final AiLlmProperties props = new AiLlmProperties("http://llm.example", "model-a", "secret-key-123",
			SEC, SEC, SEC, 2, Duration.ofMillis(500), SEC);

		assertThat(props.toString()).doesNotContain("secret-key-123");
	}
}
