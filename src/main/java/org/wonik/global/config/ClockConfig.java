package org.wonik.global.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 시간 의존 로직(세션 만료, 토큰 만료, 잠금 시각)을 테스트에서 고정·이동할 수 있도록 Clock을 주입받게 한다.
 */
@Configuration
public class ClockConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
