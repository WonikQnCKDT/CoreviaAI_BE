package org.wonik.authserver.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.wonik.authserver.domain.auth.policy.PasswordPolicy;

class TemporaryPasswordGeneratorTest {

	private final TemporaryPasswordGenerator generator = new TemporaryPasswordGenerator();

	@RepeatedTest(200)
	@DisplayName("임시 비밀번호는 항상 12자이고 비밀번호 규칙을 만족한다")
	void alwaysSatisfiesPolicy() {
		final String password = generator.generate();

		assertThat(password).hasSize(12);
		assertThat(PasswordPolicy.isSatisfiedBy(password)).isTrue();
	}
}
