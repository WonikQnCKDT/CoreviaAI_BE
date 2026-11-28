package org.wonik.authserver.domain.auth.service;

import java.time.Clock;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.wonik.authserver.domain.credential.entity.LocalCredential;
import org.wonik.authserver.domain.credential.repository.LocalCredentialRepository;
import org.wonik.global.security.InitialPasswordIssuer;

import lombok.RequiredArgsConstructor;

/** 로컬 비밀번호 기반 인증에서의 초기 임시 비밀번호 발급 구현. */
@Component
@RequiredArgsConstructor
public class LocalInitialPasswordIssuer implements InitialPasswordIssuer {

	private final LocalCredentialRepository credentialRepository;
	private final PasswordEncoder passwordEncoder;
	private final TemporaryPasswordGenerator temporaryPasswordGenerator;
	private final Clock clock;

	@Override
	public String issueInitialPassword(final Long userId) {
		final String temporaryPassword = temporaryPasswordGenerator.generate();
		final String hash = passwordEncoder.encode(temporaryPassword);
		credentialRepository.save(LocalCredential.issue(userId, hash, clock.instant()));
		return temporaryPassword;
	}
}
