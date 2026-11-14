package org.wonik.global.jwt;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * 서버 측 로그인 세션을 가리키는 JWT를 발급·검증한다. 토큰에는 사용자 ID(sub)와 세션 ID(sid)만 담는다.
 * 역할·잠금·로그아웃 여부는 토큰에 넣지 않고 세션 저장소에서 확인하므로, 토큰이 살아 있어도 세션이 끊기면 바로 막힌다.
 * <p>
 * authserver(발급)와 이후 업무 모듈(검증)이 함께 쓰므로 global에 두고 authserver 타입에 의존하지 않는다.
 */
@Component
public class JwtTokenProvider {

	private static final String CLAIM_SESSION_ID = "sid";
	private static final String CLAIM_TOKEN_TYPE = "typ";
	private static final int MIN_KEY_BYTES = 32;

	private final JwtProperties jwtProperties;
	private final Clock clock;
	private final SecretKey secretKey;
	private final JwtParser parser;

	public JwtTokenProvider(final JwtProperties jwtProperties, final Clock clock) {
		this.jwtProperties = jwtProperties;
		this.clock = clock;
		this.secretKey = createKey(jwtProperties.secret());
		this.parser = Jwts.parser()
			.verifyWith(secretKey)
			.clock(() -> Date.from(clock.instant()))
			.build();
	}

	public String createAccessToken(final Long userId, final String sessionId) {
		return create(userId, sessionId, TokenType.ACCESS, jwtProperties.accessTokenTtl().toMillis());
	}

	public String createRefreshToken(final Long userId, final String sessionId) {
		return create(userId, sessionId, TokenType.REFRESH, jwtProperties.refreshTokenTtl().toMillis());
	}

	/**
	 * 서명·만료·용도를 검증한다.
	 *
	 * @throws CustomException 만료면 {@link ErrorCode#EXPIRED_TOKEN}, 그 외 위조·형식 오류·용도 불일치는 {@link ErrorCode#INVALID_TOKEN}
	 */
	public TokenClaims parse(final String token, final TokenType expectedType) {
		final Claims claims;
		try {
			claims = parser.parseSignedClaims(token).getPayload();
		} catch (final ExpiredJwtException e) {
			throw new CustomException(ErrorCode.EXPIRED_TOKEN);
		} catch (final JwtException | IllegalArgumentException e) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}
		return toClaims(claims, expectedType);
	}

	/**
	 * 로그아웃용: 만료된 토큰이라도 서명이 맞으면 세션 ID를 꺼낸다. 위조 토큰이면 비어 있다.
	 */
	public Optional<String> extractSessionIdAllowExpired(final String token) {
		try {
			return Optional.ofNullable(parser.parseSignedClaims(token).getPayload().get(CLAIM_SESSION_ID, String.class));
		} catch (final ExpiredJwtException e) {
			return Optional.ofNullable(e.getClaims().get(CLAIM_SESSION_ID, String.class));
		} catch (final JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	private String create(final Long userId, final String sessionId, final TokenType type, final long ttlMillis) {
		final Instant now = clock.instant();
		return Jwts.builder()
			.subject(String.valueOf(userId))
			.claim(CLAIM_SESSION_ID, sessionId)
			.claim(CLAIM_TOKEN_TYPE, type.claimValue())
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plusMillis(ttlMillis)))
			.signWith(secretKey)
			.compact();
	}

	private static TokenClaims toClaims(final Claims claims, final TokenType expectedType) {
		final String type = claims.get(CLAIM_TOKEN_TYPE, String.class);
		final String sessionId = claims.get(CLAIM_SESSION_ID, String.class);
		final String subject = claims.getSubject();
		if (!expectedType.claimValue().equals(type) || sessionId == null || subject == null) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}
		try {
			return new TokenClaims(Long.valueOf(subject), sessionId);
		} catch (final NumberFormatException e) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}
	}

	private static SecretKey createKey(final String base64Secret) {
		if (base64Secret == null || base64Secret.isBlank()) {
			throw new IllegalStateException("app.jwt.secret(JWT_SECRET)이 설정되지 않았습니다");
		}
		final byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
		if (keyBytes.length < MIN_KEY_BYTES) {
			throw new IllegalStateException("JWT_SECRET은 Base64로 인코딩한 32바이트 이상의 값이어야 합니다");
		}
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
