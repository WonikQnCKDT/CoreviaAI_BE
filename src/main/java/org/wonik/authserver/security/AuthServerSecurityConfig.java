package org.wonik.authserver.security;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.wonik.authserver.domain.auth.session.AuthSessionProperties;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.security.AllowedRequest;
import org.wonik.global.security.ApiSecuritySupport;

/**
 * authserver 경로(로그인·로그아웃·재발급·내 계정·비밀번호 변경·관리자 비밀번호 초기화)의 보안 체인.
 * <p>
 * 사내 SSO로 교체할 때 이 패키지와 함께 걷어낸다. 업무 경로는 coreviaai_be 체인이 맡는다.
 */
@Configuration
@EnableConfigurationProperties(AuthSessionProperties.class)
public class AuthServerSecurityConfig {

	private static final String[] AUTH_SERVER_PATHS = {
		"/api/auth/**",
		"/api/users/me/password",
		"/api/admin/users/*/password-reset"
	};

	/** 비밀번호 변경이 강제된 세션에서도 허용: 변경 화면에 필요한 조회·변경과 로그인·로그아웃·재발급 */
	private static final List<AllowedRequest> ALLOWED_DURING_PASSWORD_CHANGE = List.of(
		new AllowedRequest(HttpMethod.POST, "/api/auth/login"),
		new AllowedRequest(HttpMethod.GET, "/api/auth/me"),
		new AllowedRequest(HttpMethod.POST, "/api/auth/logout"),
		new AllowedRequest(HttpMethod.POST, "/api/auth/refresh"),
		new AllowedRequest(HttpMethod.PATCH, "/api/users/me/password")
	);

	@Bean
	@Order(1)
	public SecurityFilterChain authServerSecurityFilterChain(
		final HttpSecurity http,
		final ApiSecuritySupport apiSecuritySupport
	) throws Exception {
		http.securityMatcher(AUTH_SERVER_PATHS);
		apiSecuritySupport.applyCommon(http, ALLOWED_DURING_PASSWORD_CHANGE);
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
			.requestMatchers("/api/admin/**").hasRole(SystemRole.SYSTEM_ADMIN.name())
			.anyRequest().authenticated()
		);
		return http.build();
	}
}
