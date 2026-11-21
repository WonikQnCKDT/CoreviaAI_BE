package org.wonik.global.aiserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.wonik.coreviaai_be.domain.wbs.client.dto.AiWbsFeasibility;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * AI 서버 호출의 봉투 해제와 오류 변환(02. AI 생성 API 공통 오류). 네트워크 대신 MockRestServiceServer로
 * 응답·예외를 결정적으로 만든다. 결과 타입은 구조가 단순한 AiWbsFeasibility를 빌려 쓴다.
 */
class AiServerClientTest {

	private static final String BASE_URL = "http://ai-server.test";
	private static final String PATH = "/generate/wbs";

	private MockRestServiceServer server;
	private AiServerClient client;

	@BeforeEach
	void setUp() {
		final RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
		server = MockRestServiceServer.bindTo(builder).build();
		client = new AiServerClient(builder.build());
	}

	@Test
	@DisplayName("성공: 요청 본문을 JSON으로 보내고 response.result만 꺼내 돌려준다")
	void unwrapsResult() {
		server.expect(requestTo(BASE_URL + PATH))
			.andExpect(method(HttpMethod.POST))
			.andExpect(content().json("{\"projectId\": 1}"))
			.andRespond(withSuccess("""
				{"success": true,
				 "response": {"status": "AI_DRAFT", "result": {"status": "AT_RISK", "reason": "범위가 넓음"}},
				 "error": null}
				""", MediaType.APPLICATION_JSON));

		final AiWbsFeasibility result = client.post(PATH, Map.of("projectId", 1), AiWbsFeasibility.class);

		assertThat(result).isEqualTo(new AiWbsFeasibility("AT_RISK", "범위가 넓음"));
		server.verify();
	}

	@Test
	@DisplayName("AI 400: BE가 요청을 잘못 만든 것이라 500 INTERNAL_SERVER_ERROR")
	void badRequestIsServerError() {
		server.expect(requestTo(BASE_URL + PATH)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

		expectError(ErrorCode.INTERNAL_SERVER_ERROR);
	}

	@Test
	@DisplayName("AI 503(Ollama 장애): 503 EXTERNAL_SERVICE_UNAVAILABLE")
	void serviceUnavailable() {
		server.expect(requestTo(BASE_URL + PATH)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

		expectError(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
	}

	@Test
	@DisplayName("AI 500(모델 결과 형식 오류): 502 AI_GENERATION_FAILED")
	void generationFailed() {
		server.expect(requestTo(BASE_URL + PATH)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		expectError(ErrorCode.AI_GENERATION_FAILED);
	}

	@Test
	@DisplayName("응답 제한 시간 초과: 504 AI_TIMEOUT")
	void timeout() {
		server.expect(requestTo(BASE_URL + PATH)).andRespond(withException(new HttpTimeoutException("read timed out")));

		expectError(ErrorCode.AI_TIMEOUT);
	}

	@Test
	@DisplayName("연결 실패: 503 EXTERNAL_SERVICE_UNAVAILABLE")
	void connectionRefused() {
		server.expect(requestTo(BASE_URL + PATH)).andRespond(withException(new ConnectException("Connection refused")));

		expectError(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
	}

	@Test
	@DisplayName("성공 상태인데 success=false거나 result가 없으면 502 AI_GENERATION_FAILED")
	void missingResult() {
		server.expect(requestTo(BASE_URL + PATH)).andRespond(withSuccess("""
			{"success": true, "response": {"status": "AI_DRAFT", "result": null}, "error": null}
			""", MediaType.APPLICATION_JSON));

		expectError(ErrorCode.AI_GENERATION_FAILED);
	}

	@Test
	@DisplayName("응답이 JSON이 아니면 502 AI_GENERATION_FAILED")
	void unreadableBody() {
		server.expect(requestTo(BASE_URL + PATH)).andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

		expectError(ErrorCode.AI_GENERATION_FAILED);
	}

	private void expectError(final ErrorCode errorCode) {
		assertThatThrownBy(() -> client.post(PATH, Map.of("projectId", 1), AiWbsFeasibility.class))
			.isInstanceOf(CustomException.class)
			.extracting(error -> ((CustomException)error).getErrorCode())
			.isEqualTo(errorCode);
	}
}
