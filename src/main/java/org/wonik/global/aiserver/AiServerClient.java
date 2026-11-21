package org.wonik.global.aiserver;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

/**
 * AI 서버(FastAPI) 내부 API 호출. 기능과 무관한 전송만 맡는다: 응답 봉투 해제와 오류 변환.
 * 어떤 경로를 어떤 DTO로 부를지는 각 도메인의 클라이언트가 정한다.
 * <p>
 * 오류 변환(02. AI 생성 API 공통 오류 기준)
 * <ul>
 *   <li>AI 400: BE가 요청을 잘못 만든 경우라 500 INTERNAL_SERVER_ERROR</li>
 *   <li>AI 503, 연결 실패: 503 EXTERNAL_SERVICE_UNAVAILABLE</li>
 *   <li>AI 500 등 그 밖의 오류, 응답 형식 오류: 502 AI_GENERATION_FAILED</li>
 *   <li>응답 제한 시간 초과: 504 AI_TIMEOUT</li>
 * </ul>
 * 자동 재시도하지 않는다. LLM 생성은 비용이 크고 AI 서버가 Ollama 호출을 이미 재시도하므로,
 * 여기서 다시 시도하면 사용자 대기 시간만 늘어난다(be-principles 외부 연동).
 */
@Slf4j
@Component
public class AiServerClient {

	private final RestClient restClient;

	public AiServerClient(@Qualifier(AiServerConfig.AI_SERVER_REST_CLIENT) final RestClient restClient) {
		this.restClient = restClient;
	}

	public <T> T post(final String path, final Object body, final Class<T> resultType) {
		final AiServerResponse<T> response;
		try {
			response = restClient.post()
				.uri(path)
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.body(responseType(resultType));
		} catch (final HttpClientErrorException e) {
			log.error("AI 서버가 BE 요청을 거부했습니다: path={}, status={}", path, e.getStatusCode().value());
			throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, e);
		} catch (final HttpServerErrorException e) {
			if (e.getStatusCode().value() == HttpStatus.SERVICE_UNAVAILABLE.value()) {
				log.warn("AI 서버를 사용할 수 없습니다: path={}", path);
				throw new CustomException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, e);
			}
			log.warn("AI 서버 생성 실패: path={}, status={}", path, e.getStatusCode().value());
			throw new CustomException(ErrorCode.AI_GENERATION_FAILED, e);
		} catch (final ResourceAccessException e) {
			if (isTimeout(e)) {
				log.warn("AI 서버 응답 시간 초과: path={}", path);
				throw new CustomException(ErrorCode.AI_TIMEOUT, e);
			}
			log.warn("AI 서버 연결 실패: path={}", path);
			throw new CustomException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, e);
		} catch (final RestClientException e) {
			log.warn("AI 서버 응답을 해석하지 못했습니다: path={}", path);
			throw new CustomException(ErrorCode.AI_GENERATION_FAILED, e);
		}
		if (response == null || !response.success() || response.response() == null
			|| response.response().result() == null) {
			log.warn("AI 서버 응답에 결과가 없습니다: path={}", path);
			throw new CustomException(ErrorCode.AI_GENERATION_FAILED);
		}
		return response.response().result();
	}

	private static <T> ParameterizedTypeReference<AiServerResponse<T>> responseType(final Class<T> resultType) {
		return ParameterizedTypeReference.forType(
			ResolvableType.forClassWithGenerics(AiServerResponse.class, resultType).getType());
	}

	private static boolean isTimeout(final Throwable error) {
		for (Throwable cause = error; cause != null; cause = cause.getCause()) {
			if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
				return true;
			}
		}
		return false;
	}
}
