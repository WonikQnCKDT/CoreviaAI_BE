package com.wonik.global.jwt;

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtUtil {
	private static final String ACCESS_TOKEN = "AccessToken";
	private static final Long ACCESS_TOKEN_EXP = (long)(30 * 1000); // 30초
	private static final String REFRESH_TOKEN = "RefreshToken";
	private static final Long REFRESH_TOKEN_EXP = (long)(24 * 60 * 60 * 1000); // 1일
	private static final String DISCORD_ID_CLAIM = "discordId";
	// TODO(ADR-0008): 배포 후 RefreshToken 수명(1일)이 지나면 제거한다.
	private static final String LEGACY_DISCORD_ID_CLAIM = "username";
	private SecretKey secretKey;

	@Value("${jwt.secret}")
	private String secretKeyBase64;

	@Value("${spring.profiles.active:}")
	private String activeProfiles;

	@PostConstruct
	public void init() {
		// 비밀 키 초기화
		final byte[] keyBytes = Decoders.BASE64.decode(secretKeyBase64);
		secretKey = Keys.hmacShaKeyFor(keyBytes);
	}

	public Duration getRefreshTokenTtl() {
		return Duration.ofMillis(REFRESH_TOKEN_EXP);
	}

	/**
	 * ADR-0001: 로그인은 discordId 인증만으로는 완료되지 않는다.
	 * botId(=선택한 채팅방)가 없는 토큰은 "선택 대기" 상태이며,
	 * /api/**는 이 상태의 토큰을 거부한다(BackEndJwtAuthFilter 참고).
	 * /auth/guild/select에서 guildId를 확정한 뒤에만 botId가 채워진
	 * 토큰이 발급된다.
	 */
	public String createAccessToken(final CustomUser user) {
		return createAccessToken(user, null);
	}

	public String createAccessToken(final CustomUser user, final String botId) {
		final var authorities = user.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.joining(","));

		final var builder = Jwts.builder()
			.claim(DISCORD_ID_CLAIM, user.getDiscordId())
			.claim("authorities", authorities);

		if (botId != null) {
			builder.claim("botId", botId);
		}

		return builder
			.subject(ACCESS_TOKEN)
			.issuedAt(new Date(System.currentTimeMillis()))
			.expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXP))
			.signWith(secretKey)
			.compact();
	}

	public String createRefreshToken(final CustomUser user) {
		return createRefreshToken(user, null);
	}

	/**
	 * botId도 RefreshToken에 함께 실어야 한다. AccessToken은 30초로 매우
	 * 짧게 만료되므로, RefreshToken에 botId가 없으면 재발급마다 "선택 대기"
	 * 상태로 되돌아가 사용자가 몇십 초마다 채팅방을 다시 선택해야 하는
	 * 문제가 생긴다.
	 */
	public String createRefreshToken(final CustomUser user, final String botId) {
		final var builder = Jwts.builder()
			.claim(DISCORD_ID_CLAIM, user.getDiscordId());

		if (botId != null) {
			builder.claim("botId", botId);
		}

		return builder
			.subject(REFRESH_TOKEN)
			.issuedAt(new Date(System.currentTimeMillis()))
			.expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXP))
			.signWith(secretKey)
			.compact();
	}

	public boolean isTokenValidate(final String token) {
		try {
			return !isTokenExpired(parseToken(token));
		} catch (final JwtException e) {
			log.error("JWT 토큰 검증 실패: {}", e.getMessage());
			return false;
		}
	}

	/**
	 * 토큰의 Discord ID를 꺼낸다.
	 * <p>
	 * ADR-0008: claim 이름을 {@code username}에서 {@code discordId}로 바꿨다. 배포 전에 발급된
	 * RefreshToken(최대 {@link #getRefreshTokenTtl()})에는 {@code username} claim만 있으므로,
	 * 그 토큰이 모두 만료될 때까지 이전 claim도 읽는다.
	 */
	public Optional<String> extractDiscordId(String token) {
		try {
			final Claims claims = parseToken(token);
			final String discordId = claims.get(DISCORD_ID_CLAIM, String.class);
			return Optional.ofNullable(discordId != null ? discordId : claims.get(LEGACY_DISCORD_ID_CLAIM, String.class));
		} catch (final JwtException e) {
			return Optional.empty();
		}
	}

	/**
	 * 토큰에 botId claim이 없으면(=아직 채팅방을 선택하지 않은 "선택 대기" 토큰)
	 * 빈 Optional을 반환한다.
	 */
	public Optional<String> extractBotId(String token) {
		try {
			return Optional.ofNullable(parseToken(token).get("botId", String.class));
		} catch (final JwtException e) {
			return Optional.empty();
		}
	}

	public ResponseCookie createAccessTokenCookie(final String accessToken) {
		boolean isProd = activeProfiles != null && activeProfiles.equals("prod");
		return ResponseCookie.from(ACCESS_TOKEN, accessToken)
				.httpOnly(true)
				.secure(isProd)  // HTTPS 연결에서만 쿠키 전송
				.path("/")
				.maxAge(ACCESS_TOKEN_EXP / 1000L)
				.build();
	}

	public ResponseCookie createRefreshTokenCookie(final String refreshToken) {
		boolean isProd = activeProfiles != null && activeProfiles.equals("prod");
		return ResponseCookie.from(REFRESH_TOKEN, refreshToken)
			.httpOnly(true)
			.secure(isProd)
			.path("/")
			.maxAge(REFRESH_TOKEN_EXP / 1000L)
			.build();
	}

	public Optional<String> extractRefreshTokenFromRequest(final HttpServletRequest request) {
		if (null == request.getCookies()) {
			return Optional.empty();
		}
		for (final Cookie cookie : request.getCookies()) {
			if (REFRESH_TOKEN.equals(cookie.getName())) {
				return Optional.of(cookie.getValue());
			}
		}
		return Optional.empty();
	}

	public Optional<String> extractAccessTokenFromRequest(final HttpServletRequest request) {
		if (null == request.getCookies()) {
			return Optional.empty();
		}
		return Arrays.stream(request.getCookies())
				.filter(c -> ACCESS_TOKEN.equals(c.getName()))
				.map(Cookie::getValue)
				.findFirst();
	}

	public ResponseCookie createExpiredRefreshTokenCookie() {
		boolean isProd = activeProfiles != null && activeProfiles.equals("prod");
		return ResponseCookie.from(REFRESH_TOKEN, "")
			.httpOnly(true)
			.secure(isProd)
			.sameSite("None")
			.path("/")
			.maxAge(0L) // 쿠키 즉시 만료
			.build();
	}

	public ResponseCookie createExpiredAccessTokenCookie() {
		boolean isProd = activeProfiles != null && activeProfiles.equals("prod");
		return ResponseCookie.from(ACCESS_TOKEN, "")
				.httpOnly(true)
				.secure(isProd)
				.sameSite("None")
				.path("/")
				.maxAge(0L) // 쿠키 즉시 만료
				.build();
	}

	private Claims parseToken(final String token) {
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token.trim())
			.getPayload();
	}

	private boolean isTokenExpired(final Claims claims) {
		final Date expiration = claims.getExpiration();
		final Date now = new Date();
//		log.info("현재 시간: " + now);
//		log.info("토큰 만료: " + expiration);
		return claims.getExpiration().before(now);
	}
}
