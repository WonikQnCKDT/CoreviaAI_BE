package org.wonik.authserver.domain.auth.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordPolicyTest {

	@ParameterizedTest
	@ValueSource(strings = {"abcd123!", "Passw0rd!", "zz9_zzzz", "a1!a1!a1!a1!"})
	@DisplayName("영문·숫자·특수문자를 모두 포함한 8자 이상이면 통과한다(대문자는 필수 아님)")
	void acceptsValidPasswords(final String password) {
		assertThat(PasswordPolicy.isSatisfiedBy(password)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"abcdefg!",    // 숫자 없음
		"abcd1234",    // 특수문자 없음
		"1234567!",    // 영문 없음
		"abc12!x",     // 7자
		"abcd 123!",   // 공백
		"비밀번호abc1!" // 비ASCII
	})
	@DisplayName("규칙을 하나라도 어기면 거부한다")
	void rejectsInvalidPasswords(final String password) {
		assertThat(PasswordPolicy.isSatisfiedBy(password)).isFalse();
	}

	@ParameterizedTest
	@NullAndEmptySource
	void rejectsNullAndEmpty(final String password) {
		assertThat(PasswordPolicy.isSatisfiedBy(password)).isFalse();
	}

	@Test
	@DisplayName("길이 경계: 64자는 통과, 65자는 거부")
	void lengthBoundary() {
		final String base = "a1!";
		final String sixtyFour = base + "a".repeat(PasswordPolicy.MAX_LENGTH - base.length());
		final String sixtyFive = sixtyFour + "a";

		assertThat(PasswordPolicy.isSatisfiedBy(sixtyFour)).isTrue();
		assertThat(PasswordPolicy.isSatisfiedBy(sixtyFive)).isFalse();
	}
}
