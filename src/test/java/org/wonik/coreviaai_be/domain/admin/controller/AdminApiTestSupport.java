package org.wonik.coreviaai_be.domain.admin.controller;

import org.junit.jupiter.api.BeforeEach;
import org.wonik.global.common.type.SystemRole;
import org.wonik.support.ApiIntegrationTestSupport;

import jakarta.servlet.http.Cookie;

/**
 * 관리자 API 통합 테스트 공통. 공통 초기화({@link ApiIntegrationTestSupport}) 뒤에
 * 시스템 관리자(ADMIN_ID) 한 명을 만들고 로그인해 둔다({@link #adminCookie}).
 */
abstract class AdminApiTestSupport extends ApiIntegrationTestSupport {

	protected static final String ADMIN_ID = "admin01";
	protected static final String ADMIN_PASSWORD = "Adm1n-pass!";

	protected Long adminId;
	protected Cookie adminCookie;

	@BeforeEach
	void loginAdmin() throws Exception {
		adminId = saveUser(ADMIN_ID, ADMIN_PASSWORD, SystemRole.SYSTEM_ADMIN, null).getId();
		adminCookie = loginOk(ADMIN_ID, ADMIN_PASSWORD);
	}
}
