package org.wonik.authserver.domain.auth.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 최초 시스템 관리자 계정 값(app.bootstrap.admin.*). 환경변수 INITIAL_ADMIN_*로만 주입한다.
 */
@ConfigurationProperties(prefix = "app.bootstrap.admin")
public record InitialAdminProperties(String loginId, String password, String name) {

	public boolean isConfigured() {
		return hasText(loginId) && hasText(password) && hasText(name);
	}

	private static boolean hasText(final String value) {
		return value != null && !value.isBlank();
	}

	@Override
	public String toString() {
		return "InitialAdminProperties[loginId=" + loginId + ", password=***, name=" + name + "]";
	}
}
