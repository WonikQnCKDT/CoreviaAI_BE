package org.wonik.coreviaai_be.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.wonik.coreviaai_be.domain.account.controller.AccountRequestController;
import org.wonik.coreviaai_be.domain.i18n.controller.LabelController;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.security.AllowedRequest;
import org.wonik.global.security.ApiSecuritySupport;

/**
 * 업무 API(/api/**, authserver 경로 제외)의 보안 체인. 로그인한 사용자만 접근하며 /api/admin/**은 시스템 관리자만 허용한다.
 * <p>
 * global 타입(ApiSecuritySupport, LoginSessionResolver)만 쓰고 authserver를 import하지 않는다.
 * 비밀번호 변경이 강제된 세션은 화면 문구 조회 외의 업무 API를 쓸 수 없다.
 * <p>
 * 공개 경로는 화면용 다국어 라벨 조회와 계정 신청·본인 상태 조회다. 신청자는 승인 전이라 로그인할 수 없다(FR-ADM-10).
 * 상태 조회는 아이디와 이름이 모두 일치하는 신청만 돌려주고, 그 외는 없는 신청과 같은 404다.
 * 승인·반려(/api/admin/account-requests)는 시스템 관리자만 가능하다.
 */
@Configuration
public class CoreviaSecurityConfig {

	/** 비밀번호 변경이 강제된 세션에도 허용: 비밀번호 변경 화면의 문구 조회 */
	private static final List<AllowedRequest> ALLOWED_DURING_PASSWORD_CHANGE = List.of(
		new AllowedRequest(HttpMethod.GET, LabelController.PATH)
	);

	@Bean
	@Order(2)
	public SecurityFilterChain coreviaSecurityFilterChain(
		final HttpSecurity http,
		final ApiSecuritySupport apiSecuritySupport
	) throws Exception {
		http.securityMatcher("/api/**");
		apiSecuritySupport.applyCommon(http, ALLOWED_DURING_PASSWORD_CHANGE);
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers(HttpMethod.GET, LabelController.PATH).permitAll()
			.requestMatchers(HttpMethod.POST, AccountRequestController.PATH).permitAll()
			.requestMatchers(HttpMethod.GET, AccountRequestController.STATUS_PATH).permitAll()
			.requestMatchers("/api/admin/**").hasRole(SystemRole.SYSTEM_ADMIN.name())
			// 시스템 관리자는 프로젝트 데이터에 접근할 수 없다(요구사항 명세서 3.1). PM 재지정 예외는 FR-PRJ-04에서 다룬다.
			.requestMatchers("/api/projects/**").hasAnyRole(SystemRole.USER.name(), SystemRole.EXECUTIVE.name())
			.anyRequest().authenticated()
		);
		return http.build();
	}
}
