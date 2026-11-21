package org.wonik.global.aiserver;

import java.net.http.HttpClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * AI 서버 전용 RestClient. 연결·응답 제한 시간을 여기서만 정해 다른 외부 호출 설정과 섞이지 않게 한다.
 */
@Configuration
@EnableConfigurationProperties(AiServerProperties.class)
public class AiServerConfig {

	public static final String AI_SERVER_REST_CLIENT = "aiServerRestClient";

	@Bean(AI_SERVER_REST_CLIENT)
	public RestClient aiServerRestClient(final RestClient.Builder builder, final AiServerProperties properties) {
		final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(properties.connectTimeout())
			.build();
		final JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.readTimeout());
		return builder
			.baseUrl(properties.baseUrl())
			.requestFactory(requestFactory)
			.build();
	}
}
