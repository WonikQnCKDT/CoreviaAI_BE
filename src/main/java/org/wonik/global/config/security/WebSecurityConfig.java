package org.wonik.global.config.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.wonik.global.jwt.JwtProperties;

/**
 * 공용 보안 설정과 마지막 순서의 기본 체인.
 * <p>
 * 보안 체인은 경로를 소유한 모듈이 갖는다
 * <ul>
 *   <li>{@code @Order(1)} authserver 경로 — {@code authserver.security.AuthServerSecurityConfig}</li>
 *   <li>{@code @Order(2)} {@code /api/**} 업무 경로 — {@code coreviaai_be.security.CoreviaSecurityConfig}</li>
 *   <li>마지막: 어느 체인에도 속하지 않는 요청 — 이 클래스. {@code /error}만 열고 나머지는 거부(deny-by-default)</li>
 * </ul>
 * 체인을 하나라도 정의하면 Spring Boot 기본 체인이 빠지므로, 이 기본 체인이 없으면 매칭되지 않는 경로가 무방비가 된다.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class WebSecurityConfig {

	@Bean
	@Order(Ordered.LOWEST_PRECEDENCE)
	public SecurityFilterChain fallbackSecurityFilterChain(
		final HttpSecurity http,
		final JsonAuthenticationEntryPoint authenticationEntryPoint,
		final JsonAccessDeniedHandler accessDeniedHandler
	) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable);
		http.formLogin(AbstractHttpConfigurer::disable);
		http.httpBasic(AbstractHttpConfigurer::disable);
		http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers("/error").permitAll()
			.anyRequest().denyAll()
		);
		http.exceptionHandling(handling -> handling
			.authenticationEntryPoint(authenticationEntryPoint)
			.accessDeniedHandler(accessDeniedHandler)
		);
		return http.build();
	}
}
