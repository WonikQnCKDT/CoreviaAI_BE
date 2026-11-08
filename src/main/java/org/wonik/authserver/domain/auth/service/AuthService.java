package org.wonik.authserver.domain.auth.service;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.authserver.domain.auth.authenticator.AuthenticationOutcome;
import org.wonik.authserver.domain.auth.dto.response.AccountResponse;
import org.wonik.authserver.domain.auth.authenticator.CredentialAuthenticator;
import org.wonik.authserver.domain.auth.session.AuthSession;
import org.wonik.authserver.domain.auth.session.AuthSessionStore;
import org.wonik.authserver.domain.credential.entity.LocalCredential;
import org.wonik.authserver.domain.credential.repository.LocalCredentialRepository;
import org.wonik.global.jwt.JwtTokenProvider;
import org.wonik.global.jwt.TokenClaims;
import org.wonik.global.jwt.TokenType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 로그인·토큰 재발급·로그아웃 흐름(UC-ADM-01).
 * <p>
 * 자격 증명 확인({@link CredentialAuthenticator})이 DB 트랜잭션 안에서 끝난 뒤에 세션 저장소(Redis)를 호출해,
 * 외부 저장소 호출이 DB 트랜잭션을 길게 잡지 않도록 한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

	private final CredentialAuthenticator credentialAuthenticator;
	private final AuthSessionStore sessionStore;
	private final JwtTokenProvider tokenProvider;
	private final UserRepository userRepository;
	private final LocalCredentialRepository credentialRepository;
	private final Clock clock;

	public IssuedLogin login(final String loginId, final String rawPassword, final String clientIp) {
		final AuthenticationOutcome outcome = credentialAuthenticator.authenticate(loginId, rawPassword, clientIp);
		if (!outcome.success()) {
			throw new CustomException(outcome.failureCode());
		}

		final AuthSession session = new AuthSession(
			UUID.randomUUID().toString(),
			outcome.userId(),
			outcome.systemRole(),
			outcome.mustChangePassword(),
			clock.instant()
		);
		sessionStore.save(session);
		return new IssuedLogin(
			tokenProvider.createAccessToken(session.userId(), session.sessionId()),
			tokenProvider.createRefreshToken(session.userId(), session.sessionId()),
			outcome
		);
	}

	/**
	 * RefreshToken으로 AccessToken을 다시 발급한다. 세션이 무활동으로 만료됐거나 끊겼으면 거부한다.
	 * 재발급 자체는 사용자 활동으로 보지 않아(세션 만료 시점을 늘리지 않음) FE 자동 갱신으로 무활동 만료가 무력화되지 않게 한다.
	 */
	public String refresh(final String refreshToken) {
		final TokenClaims claims = tokenProvider.parse(refreshToken, TokenType.REFRESH);
		final AuthSession session = sessionStore.find(claims.sessionId())
			.orElseThrow(() -> new CustomException(ErrorCode.SESSION_EXPIRED));
		if (!session.userId().equals(claims.userId())) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}
		return tokenProvider.createAccessToken(session.userId(), session.sessionId());
	}

	/** 남아 있는 토큰 중 서명이 맞는 것에서 세션을 찾아 끊는다. 이미 만료된 세션이어도 성공으로 처리한다. */
	public void logout(final Optional<String> accessToken, final Optional<String> refreshToken) {
		accessToken.flatMap(tokenProvider::extractSessionIdAllowExpired)
			.or(() -> refreshToken.flatMap(tokenProvider::extractSessionIdAllowExpired))
			.ifPresent(sessionStore::delete);
	}

	/** 내 계정 조회. 비밀번호 변경 강제 여부는 로컬 로그인 정보에서 읽는다(없으면 false). */
	@Transactional(readOnly = true)
	public AccountResponse getAccount(final Long userId) {
		final User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
		final boolean mustChangePassword = credentialRepository.findById(userId)
			.map(LocalCredential::isMustChangePassword)
			.orElse(false);
		return AccountResponse.from(user, mustChangePassword);
	}
}
