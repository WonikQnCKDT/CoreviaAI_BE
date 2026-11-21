package org.wonik.global.aiserver;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 서버(FastAPI) 호출 설정(app.ai-server.*). 값과 근거는 README 4절에 기록한다.
 * <p>
 * 생성 API는 자동 재시도하지 않으므로(1회 호출) 전체 제한 시간(deadline)은 connectTimeout + readTimeout이다.
 *
 * @param baseUrl        AI 서버 주소. 내부 네트워크 주소만 쓴다(nginx /api에 노출하지 않음)
 * @param connectTimeout 연결 수립 제한
 * @param readTimeout    응답 수신 제한. NFR-03(AI 초안 30초 이내)을 기준으로 정한다
 */
@ConfigurationProperties(prefix = "app.ai-server")
public record AiServerProperties(String baseUrl, Duration connectTimeout, Duration readTimeout) {
}
