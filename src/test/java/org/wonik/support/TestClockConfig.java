package org.wonik.support;

import java.time.Instant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * test 프로필에서 운영 Clock 대신 {@link MutableClock}을 주입한다.
 */
@Configuration
@Profile("test")
public class TestClockConfig {

	public static final Instant START = Instant.parse("2026-11-09T00:00:00Z");

	@Bean
	@Primary
	public MutableClock testClock() {
		return new MutableClock(START);
	}
}
