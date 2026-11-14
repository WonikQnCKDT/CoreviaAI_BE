package org.wonik.coreviaai_be.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.security.ApiSecuritySupport;

/**
 * 업무 API(/api/**, authserver 경로 제외)의 보안 체인. 로그인한 사용자만 접근하며 /api/admin/**은 시스템 관리자만 허용한다.
 * <p>
 * global 타입(ApiSecuritySupport, LoginSessionResolver)만 쓰고 authserver를 import하지 않는다.
 * 비밀번호 변경이 강제된 세션은 업무 API를 하나도 쓸 수 없다(허용 목록 없음).
 */
@Configuration
public class CoreviaSecurityConfig {

	@Bean
	@Order(2)
	public SecurityFilterChain coreviaSecurityFilterChain(
		final HttpSecurity http,
		final ApiSecuritySupport apiSecuritySupport
	) throws Exception {
		http.securityMatcher("/api/**");
		apiSecuritySupport.applyCommon(http, List.of());
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers("/api/admin/**").hasRole(SystemRole.SYSTEM_ADMIN.name())
			.anyRequest().authenticated()
		);
		return http.build();
	}
}
