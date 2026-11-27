package org.wonik.aiserver.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.support.MutableClock;

import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class WixAiLlmClientTest {

	private static final String OK_BODY =
		"{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"초안 JSON\"}}]}";
	private static final String API_KEY = "test-key-123";

	private final JsonMapper jsonMapper = JsonMapper.builder().build();
	private final MutableClock clock = new MutableClock(Instant.parse("2026-11-26T00:00:00Z"));
	/** 재시도 사이에 쉰 시간을 기록하고, 진짜로 기다리는 대신 가짜 시계만 앞으로 보낸다. */
	private final List<Duration> sleeps = new ArrayList<>();
	private final Sleeper sleeper = duration -> {
		sleeps.add(duration);
		clock.advance(duration);
	};

	private FakeLlmServer server;

	@BeforeEach
	void startServer() throws IOException {
		server = new FakeLlmServer();
	}

	@AfterEach
	void stopServer() {
		server.stop();
	}

	@Test
	@DisplayName("정상 응답이면 답 글자를 돌려주고, 요청에 모델·메시지·키가 담긴다")
	void success() {
		server.reply(200, OK_BODY);

		final String answer = defaultClient(2).chat("역할 지시", "처리할 내용");

		assertThat(answer).isEqualTo("초안 JSON");
		assertThat(server.calls()).isEqualTo(1);
		assertThat(sleeps).isEmpty();
		assertThat(server.lastAuthorization()).isEqualTo("Bearer " + API_KEY);
		final JsonNode sent = jsonMapper.readTree(server.requestBodies().get(0));
		assertThat(sent.path("model").asString()).isEqualTo("model-a");
		assertThat(sent.path("stream").asBoolean()).isFalse();
		assertThat(sent.path("messages").path(0).path("role").asString()).isEqualTo("system");
		assertThat(sent.path("messages").path(0).path("content").asString()).isEqualTo("역할 지시");
		assertThat(sent.path("messages").path(1).path("role").asString()).isEqualTo("user");
		assertThat(sent.path("messages").path(1).path("content").asString()).isEqualTo("처리할 내용");
	}

	@Test
	@DisplayName("주소 끝에 슬래시가 있어도 호출된다")
	void trailingSlashInBaseUrl() {
		server.reply(200, OK_BODY);
		final WixAiLlmClient client = client(
			props(server.baseUrl() + "/", API_KEY, 0, Duration.ofSeconds(2), Duration.ofSeconds(10)));

		assertThat(client.chat("s", "u")).isEqualTo("초안 JSON");
	}

	@Test
	@DisplayName("키가 비어 있으면 Authorization 머리말을 보내지 않는다")
	void noAuthorizationWithoutKey() {
		server.reply(200, OK_BODY);
		final WixAiLlmClient client = client(
			props(server.baseUrl(), "", 0, Duration.ofSeconds(2), Duration.ofSeconds(10)));

		client.chat("s", "u");

		assertThat(server.lastAuthorization()).isNull();
	}

	@Test
	@DisplayName("503이 두 번 나온 뒤 성공하면 재시도해서 성공하고, 쉬는 시간은 늘어난다")
	void retriesThenSucceeds() {
		server.reply(503, "busy");
		server.reply(503, "busy");
		server.reply(200, OK_BODY);

		final String answer = defaultClient(2).chat("s", "u");

		assertThat(answer).isEqualTo("초안 JSON");
		assertThat(server.calls()).isEqualTo(3);
		assertThat(sleeps).containsExactly(Duration.ofMillis(100), Duration.ofMillis(200));
	}

	@Test
	@DisplayName("429·502·503·504는 모두 일시 오류로 보고 재시도한다")
	void retriesAllTransientStatuses() {
		server.reply(429, "x");
		server.reply(502, "x");
		server.reply(503, "x");
		server.reply(504, "x");
		server.reply(200, OK_BODY);

		assertThat(defaultClient(4).chat("s", "u")).isEqualTo("초안 JSON");
		assertThat(server.calls()).isEqualTo(5);
		assertThat(sleeps).hasSize(4);
	}

	@Test
	@DisplayName("계속 503이면 처음 1번 + 재시도 2번만 하고 503(AI_NOT_READY)")
	void givesUpAfterMaxRetries() {
		server.reply(503, "busy");

		assertErrorCode(() -> defaultClient(2).chat("s", "u"), ErrorCode.AI_NOT_READY);

		assertThat(server.calls()).isEqualTo(3);
		assertThat(sleeps).hasSize(2);
	}

	@Test
	@DisplayName("재시도 횟수가 0이면 한 번만 시도한다")
	void noRetriesWhenMaxRetriesIsZero() {
		server.reply(503, "busy");

		assertErrorCode(() -> defaultClient(0).chat("s", "u"), ErrorCode.AI_NOT_READY);

		assertThat(server.calls()).isEqualTo(1);
		assertThat(sleeps).isEmpty();
	}

	@Test
	@DisplayName("400 같은 요청 오류는 재시도하지 않고 바로 실패하며, 응답 본문과 키는 오류에 담기지 않는다")
	void doesNotRetryClientErrors() {
		server.reply(400, "SECRET-BODY-xyz");

		assertThatThrownBy(() -> defaultClient(2).chat("s", "u"))
			.isInstanceOf(CustomException.class)
			.extracting("errorCode").isEqualTo(ErrorCode.AI_NOT_READY);
		assertThatThrownBy(() -> defaultClient(2).chat("s", "u"))
			.hasMessageNotContaining("SECRET-BODY-xyz")
			.hasMessageNotContaining(API_KEY)
			.hasNoCause();

		assertThat(server.calls()).isEqualTo(2);
		assertThat(sleeps).isEmpty();
	}

	@Test
	@DisplayName("응답을 기다리다 시간이 지나면 재시도 없이 504(AI_TIMEOUT)")
	void readTimeoutIsNotRetried() {
		// 진짜 시간이 필요한 유일한 시험: 서버가 0.6초 뒤에 답하고, 클라이언트는 0.1초만 기다린다
		server.reply(200, OK_BODY, 600);
		final WixAiLlmClient client = client(
			props(server.baseUrl(), API_KEY, 2, Duration.ofMillis(100), Duration.ofSeconds(10)));

		assertErrorCode(() -> client.chat("s", "u"), ErrorCode.AI_TIMEOUT);

		assertThat(server.calls()).isEqualTo(1);
		assertThat(sleeps).isEmpty();
	}

	@Test
	@DisplayName("재시도까지 합친 전체 제한 시간을 넘기게 되면 504(AI_TIMEOUT)")
	void overallDeadlineExceeded() {
		server.reply(503, "busy");
		final WixAiLlmClient client = client(
			props(server.baseUrl(), API_KEY, 5, Duration.ofSeconds(2), Duration.ofMillis(250)));

		assertErrorCode(() -> client.chat("s", "u"), ErrorCode.AI_TIMEOUT);

		// 첫 대기(100ms)는 마감 안이라 쉬고, 두 번째 대기(200ms)는 남은 150ms보다 길어서 포기한다
		assertThat(server.calls()).isEqualTo(2);
		assertThat(sleeps).containsExactly(Duration.ofMillis(100));
	}

	@Test
	@DisplayName("서버에 연결할 수 없으면 재시도한 뒤 503(AI_NOT_READY)")
	void connectionRefused() throws IOException {
		final String deadUrl = "http://127.0.0.1:" + freePort();
		final WixAiLlmClient client = client(
			props(deadUrl, API_KEY, 2, Duration.ofSeconds(2), Duration.ofSeconds(10)));

		assertErrorCode(() -> client.chat("s", "u"), ErrorCode.AI_NOT_READY);

		assertThat(sleeps).hasSize(2);
	}

	@Test
	@DisplayName("응답 모양이 이상하면(JSON 아님·choices 없음·content null·공백) 재시도 없이 502")
	void malformedResponses() {
		server.reply(200, "JSON이 아님");
		server.reply(200, "{\"choices\":[]}");
		server.reply(200, "{\"choices\":[{\"message\":{\"content\":null}}]}");
		server.reply(200, "{\"choices\":[{\"message\":{\"content\":\"   \"}}]}");
		final WixAiLlmClient client = defaultClient(2);

		for (int i = 0; i < 4; i++) {
			assertErrorCode(() -> client.chat("s", "u"), ErrorCode.AI_GENERATION_FAILED);
		}

		assertThat(server.calls()).isEqualTo(4);
		assertThat(sleeps).isEmpty();
	}

	@Test
	@DisplayName("주소나 모델이 비어 있으면 서버를 부르지 않고 503(AI_NOT_READY)")
	void notConfigured() {
		final WixAiLlmClient noUrl = client(
			props(null, API_KEY, 2, Duration.ofSeconds(2), Duration.ofSeconds(10)));
		final WixAiLlmClient blankUrl = client(
			props("  ", API_KEY, 2, Duration.ofSeconds(2), Duration.ofSeconds(10)));

		assertErrorCode(() -> noUrl.chat("s", "u"), ErrorCode.AI_NOT_READY);
		assertErrorCode(() -> blankUrl.chat("s", "u"), ErrorCode.AI_NOT_READY);

		assertThat(server.calls()).isZero();
	}

	@Test
	@DisplayName("주소 형식이 잘못되면(스킴 없음·http 아님·호스트 없음) 서버를 부르지 않고 503(AI_NOT_READY)")
	void invalidBaseUrl() {
		for (final String url : List.of("llm.example", "ftp://llm.example", "file:///etc/passwd", "http://")) {
			final WixAiLlmClient client = client(
				props(url, API_KEY, 2, Duration.ofSeconds(2), Duration.ofSeconds(10)));

			assertErrorCode(() -> client.chat("s", "u"), ErrorCode.AI_NOT_READY);
		}

		assertThat(server.calls()).isZero();
	}

	@Test
	@DisplayName("지시문이 null이거나 처리할 내용이 비어 있으면 서버를 부르지 않고 거부")
	void rejectsEmptyPrompts() {
		final WixAiLlmClient client = defaultClient(2);

		assertThatThrownBy(() -> client.chat(null, "u")).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> client.chat("s", null)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> client.chat("s", "  ")).isInstanceOf(IllegalArgumentException.class);

		assertThat(server.calls()).isZero();
	}

	private static void assertErrorCode(final ThrowingCallable call, final ErrorCode expected) {
		assertThatThrownBy(call)
			.isInstanceOf(CustomException.class)
			.extracting("errorCode").isEqualTo(expected);
	}

	private AiLlmProperties props(final String baseUrl, final String apiKey, final int maxRetries,
		final Duration readTimeout, final Duration overallDeadline) {
		return new AiLlmProperties(baseUrl, "model-a", apiKey, Duration.ofSeconds(1), readTimeout,
			overallDeadline, maxRetries, Duration.ofMillis(200), Duration.ofSeconds(1));
	}

	private WixAiLlmClient client(final AiLlmProperties properties) {
		final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(properties.connectTimeout())
			.version(HttpClient.Version.HTTP_1_1)
			.build();
		// 무작위 값은 항상 0으로 고정해 쉬는 시간을 예측 가능하게 한다(상한의 절반)
		return new WixAiLlmClient(properties, jsonMapper, clock, httpClient, sleeper, () -> 0L);
	}

	private WixAiLlmClient defaultClient(final int maxRetries) {
		return client(props(server.baseUrl(), API_KEY, maxRetries, Duration.ofSeconds(2), Duration.ofSeconds(10)));
	}

	private static int freePort() throws IOException {
		try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
			return socket.getLocalPort();
		}
	}

	/** 이 컴퓨터 안에서만 잠깐 뜨는 가짜 LLM 서버. 준비해 둔 답을 순서대로 돌려주고, 마지막 답은 반복한다. */
	private static final class FakeLlmServer {

		private record Reply(int status, String body, long delayMillis) {
		}

		private final HttpServer httpServer;
		private final List<Reply> replies = new CopyOnWriteArrayList<>();
		private final AtomicInteger calls = new AtomicInteger();
		private final List<String> requestBodies = Collections.synchronizedList(new ArrayList<>());
		private volatile String lastAuthorization;

		FakeLlmServer() throws IOException {
			httpServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
			httpServer.createContext("/chat/completions", exchange -> {
				final int index = calls.getAndIncrement();
				requestBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
				lastAuthorization = exchange.getRequestHeaders().getFirst("Authorization");
				final Reply reply = replies.get(Math.min(index, replies.size() - 1));
				try {
					if (reply.delayMillis() > 0) {
						Thread.sleep(reply.delayMillis());
					}
					final byte[] bytes = reply.body().getBytes(StandardCharsets.UTF_8);
					exchange.getResponseHeaders().add("Content-Type", "application/json");
					exchange.sendResponseHeaders(reply.status(), bytes.length);
					exchange.getResponseBody().write(bytes);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (IOException e) {
					// 클라이언트가 기다리다 먼저 끊은 경우(시간 초과 시험)라서 무시한다
				} finally {
					exchange.close();
				}
			});
			httpServer.start();
		}

		void reply(final int status, final String body) {
			reply(status, body, 0);
		}

		void reply(final int status, final String body, final long delayMillis) {
			replies.add(new Reply(status, body, delayMillis));
		}

		String baseUrl() {
			return "http://127.0.0.1:" + httpServer.getAddress().getPort();
		}

		int calls() {
			return calls.get();
		}

		List<String> requestBodies() {
			return requestBodies;
		}

		String lastAuthorization() {
			return lastAuthorization;
		}

		void stop() {
			httpServer.stop(0);
		}
	}
}