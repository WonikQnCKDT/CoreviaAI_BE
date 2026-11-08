package org.wonik.global.config;

import java.time.Clock;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * createdAt·updatedAt이 주입된 Clock을 따르도록 auditing 시각 공급자를 지정한다.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

	@Bean
	public DateTimeProvider auditingDateTimeProvider(final Clock clock) {
		return () -> Optional.of(clock.instant());
	}
}
