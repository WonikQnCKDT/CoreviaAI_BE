package org.wonik.global.security;

import java.util.List;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;
import org.wonik.global.config.security.JsonAccessDeniedHandler;
import org.wonik.global.config.security.JsonAuthenticationEntryPoint;
import org.wonik.global.config.security.SecurityErrorResponseWriter;
import org.wonik.global.jwt.AuthCookieManager;
import org.wonik.global.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

/**
 * 모듈별 API 보안 체인이 공통으로 쓰는 설정. 경로 매칭과 인가 규칙은 각 모듈 체인이 정한다.
 * <ul>
 *   <li>인증 상태는 서블릿 세션이 아니라 JWT 쿠키 + 세션 저장소로 판단하므로 STATELESS</li>
 *   <li>CSRF 토큰 대신 SameSite=Lax 쿠키와 same-origin 배포(nginx)로 막는다(AuthCookieManager 참고)</li>
 *   <li>401·403을 ApiResponse JSON으로 응답</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ApiSecuritySupport {

	private final AuthCookieManager cookieManager;
	private final JwtTokenProvider tokenProvider;
	private final LoginSessionResolver sessionResolver;
	private final SecurityErrorResponseWriter errorResponseWriter;
	private final JsonAuthenticationEntryPoint authenticationEntryPoint;
	private final JsonAccessDeniedHandler accessDeniedHandler;

	/**
	 * @param allowedDuringPasswordChange 비밀번호 변경이 강제된 세션에도 허용할 요청(이 체인 경로 중에서)
	 */
	public void applyCommon(final HttpSecurity http, final List<AllowedRequest> allowedDuringPasswordChange) {
		http.csrf(AbstractHttpConfigurer::disable);
		http.formLogin(AbstractHttpConfigurer::disable);
		http.httpBasic(AbstractHttpConfigurer::disable);
		http.logout(AbstractHttpConfigurer::disable);
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.exceptionHandling(handling -> handling
			.authenticationEntryPoint(authenticationEntryPoint)
			.accessDeniedHandler(accessDeniedHandler)
		);
		http.addFilterBefore(
			new JwtAuthenticationFilter(
				cookieManager,
				tokenProvider,
				sessionResolver,
				errorResponseWriter,
				allowedDuringPasswordChange
			),
			UsernamePasswordAuthenticationFilter.class
		);
	}
}
