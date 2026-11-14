package org.wonik.global.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.config.security.JsonAuthenticationEntryPoint;
import org.wonik.global.config.security.SecurityErrorResponseWriter;
import org.wonik.global.jwt.AuthCookieManager;
import org.wonik.global.jwt.JwtTokenProvider;
import org.wonik.global.jwt.LoginUser;
import org.wonik.global.jwt.TokenClaims;
import org.wonik.global.jwt.TokenType;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * AccessToken 쿠키 → 서명·만료 검증 → {@link LoginSessionResolver}로 세션 확인·활동 갱신 → SecurityContext 설정.
 * <p>
 * 실패해도 여기서 바로 막지 않고 사유만 request attribute에 남긴다. 인증이 필요한 경로면 Security가
 * {@link JsonAuthenticationEntryPoint}로 그 사유를 응답하고, 로그인처럼 열린 경로면 그대로 통과한다.
 * <p>
 * 비밀번호 변경이 강제된 세션은 체인별로 넘겨받은 허용 요청 외에는 403 PASSWORD_CHANGE_REQUIRED로 막는다.
 * 각 모듈의 보안 체인이 직접 생성해 쓰며 Spring bean으로 등록하지 않는다(서블릿 filter 중복 등록 방지).
 */
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final AuthCookieManager cookieManager;
	private final JwtTokenProvider tokenProvider;
	private final LoginSessionResolver sessionResolver;
	private final SecurityErrorResponseWriter errorResponseWriter;
	private final List<AllowedRequest> allowedDuringPasswordChange;

	public JwtAuthenticationFilter(
		final AuthCookieManager cookieManager,
		final JwtTokenProvider tokenProvider,
		final LoginSessionResolver sessionResolver,
		final SecurityErrorResponseWriter errorResponseWriter,
		final List<AllowedRequest> allowedDuringPasswordChange
	) {
		this.cookieManager = cookieManager;
		this.tokenProvider = tokenProvider;
		this.sessionResolver = sessionResolver;
		this.errorResponseWriter = errorResponseWriter;
		this.allowedDuringPasswordChange = List.copyOf(allowedDuringPasswordChange);
	}

	@Override
	protected void doFilterInternal(
		final HttpServletRequest request,
		final HttpServletResponse response,
		final FilterChain filterChain
	) throws ServletException, IOException {
		final Optional<String> accessToken = cookieManager.readAccessToken(request);
		if (accessToken.isEmpty()) {
			filterChain.doFilter(request, response);
			return;
		}

		final Optional<LoginSession> session;
		try {
			session = resolveSession(accessToken.get(), request);
		} catch (final DataAccessException e) {
			log.error("세션 저장소에 연결할 수 없습니다", e);
			errorResponseWriter.write(response, ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
			return;
		}

		if (session.isPresent()) {
			if (session.get().mustChangePassword() && !isAllowedDuringPasswordChange(request)) {
				errorResponseWriter.write(response, ErrorCode.PASSWORD_CHANGE_REQUIRED);
				return;
			}
			authenticate(session.get().user());
		}
		filterChain.doFilter(request, response);
	}

	private Optional<LoginSession> resolveSession(final String accessToken, final HttpServletRequest request) {
		final TokenClaims claims;
		try {
			claims = tokenProvider.parse(accessToken, TokenType.ACCESS);
		} catch (final CustomException e) {
			request.setAttribute(JsonAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE, e.getErrorCode());
			return Optional.empty();
		}

		final Optional<LoginSession> session = sessionResolver.resolveAndTouch(claims);
		if (session.isEmpty()) {
			request.setAttribute(JsonAuthenticationEntryPoint.ERROR_CODE_ATTRIBUTE, ErrorCode.SESSION_EXPIRED);
		}
		return session;
	}

	private static void authenticate(final LoginUser user) {
		final UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
			user,
			null,
			List.of(new SimpleGrantedAuthority(user.systemRole().authority()))
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private boolean isAllowedDuringPasswordChange(final HttpServletRequest request) {
		final String path = request.getRequestURI().substring(request.getContextPath().length());
		return allowedDuringPasswordChange.stream().anyMatch(allowed -> allowed.matches(request.getMethod(), path));
	}
}
