package org.wonik.authserver.domain.auth.authenticator;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.authserver.domain.auth.loginhistory.LoginFailureReason;
import org.wonik.authserver.domain.auth.loginhistory.LoginHistory;
import org.wonik.authserver.domain.auth.loginhistory.LoginHistoryRepository;
import org.wonik.authserver.domain.auth.policy.LoginLockPolicy;
import org.wonik.authserver.domain.credential.entity.LocalCredential;
import org.wonik.authserver.domain.credential.repository.LocalCredentialRepository;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * 로컬 계정 ID/PW 확인(UC-ADM-01 2단계, 2a~2c).
 * <p>
 * 사용자(users)와 로컬 로그인 정보(local_credentials)를 이 순서로 비관적 잠금으로 읽어 판정한다.
 * 판정 순서: 없는 ID·로그인 정보 없음 → 잠긴 계정 → 비밀번호 불일치(실패 횟수 증가, 5회째 잠금) → 비활성 계정 → 성공.
 * 비활성 여부는 비밀번호가 맞은 뒤에만 알려 계정 상태가 노출되는 범위를 줄인다.
 * 결과와 접속 로그를 한 트랜잭션에 기록한다.
 */
@Slf4j
@Component
public class LocalCredentialAuthenticator implements CredentialAuthenticator {

	private final UserRepository userRepository;
	private final LocalCredentialRepository credentialRepository;
	private final LoginHistoryRepository loginHistoryRepository;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	/** 없는 ID에도 해시 비교 시간을 들여 응답 시간으로 계정 존재를 추측하기 어렵게 한다. */
	private final String timingDummyHash;

	public LocalCredentialAuthenticator(
		final UserRepository userRepository,
		final LocalCredentialRepository credentialRepository,
		final LoginHistoryRepository loginHistoryRepository,
		final PasswordEncoder passwordEncoder,
		final Clock clock
	) {
		this.userRepository = userRepository;
		this.credentialRepository = credentialRepository;
		this.loginHistoryRepository = loginHistoryRepository;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
		this.timingDummyHash = passwordEncoder.encode("timing-dummy-password-1!");
	}

	@Override
	@Transactional
	public AuthenticationOutcome authenticate(final String loginId, final String rawPassword, final String clientIp) {
		final Instant now = clock.instant();
		final Optional<User> foundUser = userRepository.findByLoginIdForUpdate(loginId);
		if (foundUser.isEmpty()) {
			passwordEncoder.matches(rawPassword, timingDummyHash);
			return fail(null, loginId, LoginFailureReason.UNKNOWN_LOGIN_ID, clientIp, now, ErrorCode.INVALID_CREDENTIALS);
		}

		final User user = foundUser.get();
		final Optional<LocalCredential> foundCredential = credentialRepository.findByUserIdForUpdate(user.getId());
		if (foundCredential.isEmpty()) {
			passwordEncoder.matches(rawPassword, timingDummyHash);
			return fail(user.getId(), loginId, LoginFailureReason.NO_LOCAL_CREDENTIAL, clientIp, now, ErrorCode.INVALID_CREDENTIALS);
		}

		final LocalCredential credential = foundCredential.get();
		if (credential.isLocked()) {
			return fail(user.getId(), loginId, LoginFailureReason.ACCOUNT_LOCKED, clientIp, now, ErrorCode.ACCOUNT_LOCKED);
		}

		if (!passwordEncoder.matches(rawPassword, credential.getPasswordHash())) {
			final boolean lockedNow = credential.recordLoginFailure(LoginLockPolicy.MAX_CONSECUTIVE_FAILURES, now);
			if (lockedNow) {
				log.warn("로그인 연속 실패로 계정 잠금 - userId={}", user.getId());
				return fail(user.getId(), loginId, LoginFailureReason.BAD_PASSWORD, clientIp, now, ErrorCode.ACCOUNT_LOCKED);
			}
			return fail(user.getId(), loginId, LoginFailureReason.BAD_PASSWORD, clientIp, now, ErrorCode.INVALID_CREDENTIALS);
		}

		if (!user.isActive()) {
			return fail(user.getId(), loginId, LoginFailureReason.ACCOUNT_DISABLED, clientIp, now, ErrorCode.ACCOUNT_DISABLED);
		}

		credential.recordLoginSuccess();
		user.recordLogin(now);
		loginHistoryRepository.save(LoginHistory.success(user.getId(), loginId, clientIp, now));
		return AuthenticationOutcome.succeeded(
			user.getId(),
			user.getLoginId(),
			user.getName(),
			user.getSystemRole(),
			credential.isMustChangePassword()
		);
	}

	private AuthenticationOutcome fail(
		final Long userId,
		final String loginId,
		final LoginFailureReason reason,
		final String clientIp,
		final Instant now,
		final ErrorCode errorCode
	) {
		loginHistoryRepository.save(LoginHistory.failure(userId, loginId, reason, clientIp, now));
		return AuthenticationOutcome.failed(errorCode);
	}
}
