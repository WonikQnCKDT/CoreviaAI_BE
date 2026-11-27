package org.wonik.aiserver.llm;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * OpenAI 호환 API(/chat/completions)로 Wix AI(BizRouter) LLM을 호출하는 클라이언트.
 * 시간 제한(연결·응답·전체)과 재시도(일시 오류만, 지수 백오프 + 지터)를 여기서만 처리해서
 * 기능 코드는 HTTP나 재시도를 몰라도 되게 한다.
 */
@Component
@EnableConfigurationProperties(AiLlmProperties.class)
public class WixAiLlmClient implements LlmClient {

	private static final Logger log = LoggerFactory.getLogger(WixAiLlmClient.class);

	/** LLM 서버가 잠깐 바쁘거나 재시작 중일 때 나오는 응답이라 다시 보내 볼 가치가 있다. */
	private static final Set<Integer> RETRYABLE_STATUS = Set.of(429, 502, 503, 504);

	private final AiLlmProperties properties;
	private final JsonMapper jsonMapper;
	private final Clock clock;
	private final HttpClient httpClient;
	private final Sleeper sleeper;
	private final RandomGenerator random;

	@Autowired
	public WixAiLlmClient(final AiLlmProperties properties, final JsonMapper jsonMapper,
		final Clock clock) {
		// 서버에서는 하나의 객체를 여러 요청이 함께 쓰므로, 스레드마다 안전한 무작위 값을 쓴다
		this(properties, jsonMapper, clock, newHttpClient(properties), Sleeper.SYSTEM,
			() -> ThreadLocalRandom.current().nextLong());
	}

	/** 테스트에서 가짜 쉬기·가짜 무작위를 끼우기 위한 생성자. */
	WixAiLlmClient(final AiLlmProperties properties, final JsonMapper jsonMapper, final Clock clock,
		final HttpClient httpClient, final Sleeper sleeper, final RandomGenerator random) {
		this.properties = properties;
		this.jsonMapper = jsonMapper;
		this.clock = clock;
		this.httpClient = httpClient;
		this.sleeper = sleeper;
		this.random = random;
	}

	@Override
	public String chat(final String systemPrompt, final String userPrompt) {
		if (systemPrompt == null || userPrompt == null || userPrompt.isBlank()) {
			throw new IllegalArgumentException("지시문과 처리할 내용은 비어 있을 수 없다");
		}
		if (!properties.isConfigured()) {
			throw new CustomException(ErrorCode.AI_NOT_READY);
		}

		final URI endpoint = endpoint();
		final String body = requestBody(systemPrompt, userPrompt);
		final Instant deadline = clock.instant().plus(properties.overallDeadline());
		Throwable lastFailure = null;

		for (int attempt = 0;; attempt++) {
			final Duration remaining = Duration.between(clock.instant(), deadline);
			if (remaining.isZero() || remaining.isNegative()) {
				throw new CustomException(ErrorCode.AI_TIMEOUT, lastFailure);
			}

			String failureReason;
			try {
				final Duration readLimit = remaining.compareTo(properties.readTimeout()) < 0
					? remaining
					: properties.readTimeout();
				final HttpResponse<String> response = httpClient.send(
					request(endpoint, body, readLimit), HttpResponse.BodyHandlers.ofString());
				final int status = response.statusCode();
				if (status >= 200 && status < 300) {
					return extractContent(response.body());
				}
				if (!RETRYABLE_STATUS.contains(status)) {
					// 요청 오류·인증 오류는 다시 보내도 결과가 같다. 응답 본문은 남기지 않는다.
					log.warn("LLM 호출이 거절되었습니다. status={}", status);
					throw new CustomException(ErrorCode.AI_NOT_READY);
				}
				failureReason = "status " + status;
				lastFailure = new IllegalStateException("LLM 일시 오류 " + failureReason);
			} catch (HttpConnectTimeoutException | ConnectException e) {
				failureReason = e.getClass().getSimpleName();
				lastFailure = e;
			} catch (HttpTimeoutException e) {
				// 모델이 답을 만드는 도중 시간이 다 된 것. 다시 보내도 또 오래 걸릴 가능성이 커서 재시도하지 않는다.
				throw new CustomException(ErrorCode.AI_TIMEOUT, e);
			} catch (IOException e) {
				failureReason = e.getClass().getSimpleName();
				lastFailure = e;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new CustomException(ErrorCode.AI_NOT_READY, e);
			}

			if (attempt >= properties.maxRetries()) {
				throw new CustomException(ErrorCode.AI_NOT_READY, lastFailure);
			}
			final Duration delay = backoffDelay(attempt);
			if (delay.compareTo(Duration.between(clock.instant(), deadline)) >= 0) {
				throw new CustomException(ErrorCode.AI_TIMEOUT, lastFailure);
			}
			log.warn("LLM 호출 실패, 재시도합니다. attempt={}/{}, reason={}",
				attempt + 1, properties.maxRetries() + 1, failureReason);
			sleep(delay);
		}
	}

	private static HttpClient newHttpClient(final AiLlmProperties properties) {
		return HttpClient.newBuilder()
			.connectTimeout(properties.connectTimeout())
			// 일부 서버가 HTTP/2 업그레이드 요청을 거부하므로 1.1로 고정한다
			.version(HttpClient.Version.HTTP_1_1)
			.build();
	}

	private URI endpoint() {
		final String base = properties.baseUrl().trim();
		final String normalized = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
		final URI uri;
		try {
			uri = URI.create(normalized + "/chat/completions");
		} catch (IllegalArgumentException e) {
			// 주소 설정이 잘못된 것이므로 준비되지 않은 상태로 본다
			throw new CustomException(ErrorCode.AI_NOT_READY, e);
		}
		// http·https가 아니거나 호스트가 없으면 설정 오류다. 그대로 두면 HttpClient가 IllegalArgumentException을 던져 500이 된다
		final String scheme = uri.getScheme();
		if (uri.getHost() == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
			throw new CustomException(ErrorCode.AI_NOT_READY);
		}
		return uri;
	}

	private HttpRequest request(final URI endpoint, final String body, final Duration timeout) {
		final HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
			.timeout(timeout)
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body));
		if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
			builder.header("Authorization", "Bearer " + properties.apiKey());
		}
		return builder.build();
	}

	private String requestBody(final String systemPrompt, final String userPrompt) {
		final Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("model", properties.model());
		payload.put("stream", false);
		payload.put("messages", List.of(
			Map.of("role", "system", "content", systemPrompt),
			Map.of("role", "user", "content", userPrompt)));
		return jsonMapper.writeValueAsString(payload);
	}

	/** 응답 JSON에서 choices[0].message.content만 꺼낸다. 모양이 다르면 생성 실패(502)로 본다. */
	private String extractContent(final String responseBody) {
		try {
			final JsonNode root = jsonMapper.readTree(responseBody);
			final JsonNode content = root.path("choices").path(0).path("message").path("content");
			if (content.isMissingNode() || content.isNull() || !content.isValueNode()) {
				throw new CustomException(ErrorCode.AI_GENERATION_FAILED);
			}
			final String text = content.asString();
			if (text == null || text.isBlank()) {
				throw new CustomException(ErrorCode.AI_GENERATION_FAILED);
			}
			logUsage(root.path("usage"));
			return text;
		} catch (JacksonException e) {
			throw new CustomException(ErrorCode.AI_GENERATION_FAILED, e);
		}
	}

	/** 사용한 토큰 수만 남긴다. 질문·답 내용이나 키는 남기지 않는다. 서버가 값을 안 주면 ?로 표시한다. */
	private void logUsage(final JsonNode usage) {
		log.info("LLM 사용량: model={}, promptTokens={}, completionTokens={}, totalTokens={}",
			properties.model(),
			tokenCount(usage.path("prompt_tokens")),
			tokenCount(usage.path("completion_tokens")),
			tokenCount(usage.path("total_tokens")));
	}

	private static String tokenCount(final JsonNode node) {
		return node.isNumber() ? node.asString() : "?";
	}

	/**
	 * 지수 백오프 + 지터. 상한을 0.5 → 1 → 2초…로 늘리고(최대 retryMaxDelay), 그 절반~전체 사이에서 무작위로 고른다.
	 * 여러 요청이 동시에 실패해도 같은 순간에 한꺼번에 재시도하지 않게 하기 위함이다.
	 */
	private Duration backoffDelay(final int attempt) {
		final long capMillis = Math.min(properties.retryMaxDelay().toMillis(),
			properties.retryInitialDelay().toMillis() * (1L << Math.min(attempt, 20)));
		final long halfMillis = capMillis / 2;
		return Duration.ofMillis(halfMillis + (long) (random.nextDouble() * (capMillis - halfMillis)));
	}

	private void sleep(final Duration delay) {
		try {
			sleeper.sleep(delay);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new CustomException(ErrorCode.AI_NOT_READY, e);
		}
	}
}
