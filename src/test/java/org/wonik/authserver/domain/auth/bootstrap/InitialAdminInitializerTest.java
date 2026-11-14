package org.wonik.authserver.domain.auth.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.wonik.authserver.domain.credential.entity.LocalCredential;
import org.wonik.authserver.domain.credential.repository.LocalCredentialRepository;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;
import org.wonik.support.MutableClock;

class InitialAdminInitializerTest {

	private final UserRepository userRepository = mock(UserRepository.class);
	private final LocalCredentialRepository credentialRepository = mock(LocalCredentialRepository.class);
	private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
	private final MutableClock clock = new MutableClock(Instant.parse("2026-11-09T00:00:00Z"));

	private InitialAdminInitializer initializer(final String loginId, final String password, final String name) {
		return new InitialAdminInitializer(
			new InitialAdminProperties(loginId, password, name),
			userRepository,
			credentialRepository,
			passwordEncoder,
			clock
		);
	}

	private void run(final InitialAdminInitializer initializer) {
		initializer.run(new DefaultApplicationArguments());
	}

	@Test
	@DisplayName("관리자가 이미 있으면 환경변수가 있어도 만들지 않는다")
	void skipsWhenAdminExists() {
		when(userRepository.existsBySystemRole(SystemRole.SYSTEM_ADMIN)).thenReturn(true);

		run(initializer("admin", "Adm1n-pass!", "관리자"));

		verify(userRepository, never()).save(any());
		verify(credentialRepository, never()).save(any());
	}

	@Test
	@DisplayName("관리자가 없고 환경변수도 없으면 경고만 남기고 기동을 계속한다")
	void skipsWhenNotConfigured() {
		run(initializer("", "", ""));

		verify(userRepository, never()).save(any());
	}

	@Test
	@DisplayName("비밀번호가 규칙에 맞지 않으면 기동을 멈춘다")
	void failsOnWeakPassword() {
		assertThatThrownBy(() -> run(initializer("admin", "weakpass", "관리자")))
			.isInstanceOf(IllegalStateException.class);
		verify(userRepository, never()).save(any());
	}

	@Test
	@DisplayName("첫 관리자는 사용자와 로컬 로그인 정보(해시·변경 강제)로 나눠 만들어진다")
	void createsAdminUserAndCredential() {
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
			final User user = invocation.getArgument(0);
			ReflectionTestUtils.setField(user, "id", 10L);
			return user;
		});

		run(initializer("admin", "Adm1n-pass!", "관리자"));

		final ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
		verify(userRepository).save(user.capture());
		assertThat(user.getValue().getSystemRole()).isEqualTo(SystemRole.SYSTEM_ADMIN);

		final ArgumentCaptor<LocalCredential> credential = ArgumentCaptor.forClass(LocalCredential.class);
		verify(credentialRepository).save(credential.capture());
		assertThat(credential.getValue().getUserId()).isEqualTo(10L);
		assertThat(credential.getValue().isMustChangePassword()).isTrue();
		assertThat(passwordEncoder.matches("Adm1n-pass!", credential.getValue().getPasswordHash())).isTrue();
	}
}
