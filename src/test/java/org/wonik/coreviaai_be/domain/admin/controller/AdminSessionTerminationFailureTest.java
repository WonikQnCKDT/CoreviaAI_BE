package org.wonik.coreviaai_be.domain.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.department.entity.Department;
import org.wonik.global.security.UserSessionTerminator;
import org.wonik.global.user.entity.User;

import jakarta.servlet.http.Cookie;

/**
 * 부분 실패: DB 변경은 커밋됐는데 세션 저장소 장애로 세션을 끊지 못한 경우.
 * 503으로 알리고, 같은 요청을 다시 보내면 DB·이력은 그대로 두고 세션만 끊는지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSessionTerminationFailureTest extends AdminApiTestSupport {

	@MockitoSpyBean
	private UserSessionTerminator sessionTerminator;

	@Test
	@DisplayName("비활성화 후 세션 끊기가 실패하면 503, 재전송하면 세션만 끊기고 이력은 한 번만 남는다")
	void retryAfterSessionStoreFailure() throws Exception {
		final Department team = saveDepartment("개발팀", null);
		final User user = saveUser("user01", USER_PASSWORD, SystemRole.USER, team.getId());
		final Cookie userCookie = loginOk("user01", USER_PASSWORD);
		doThrow(new DataAccessResourceFailureException("세션 저장소 연결 실패"))
			.doCallRealMethod()
			.when(sessionTerminator).terminateAllSessions(anyLong());

		expectError(
			patchJson(adminCookie, body("status", "INACTIVE"), "/api/admin/users/{id}/status", user.getId()),
			ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE
		);
		assertThat(findUser(user.getId()).isActive()).isFalse();
		getAs(userCookie, "/api/auth/me").andExpect(status().isOk());

		patchJson(adminCookie, body("status", "INACTIVE"), "/api/admin/users/{id}/status", user.getId())
			.andExpect(status().isOk());
		expectError(getAs(userCookie, "/api/auth/me"), ErrorCode.SESSION_EXPIRED);
		assertThat(auditLogs(AuditAction.USER_DEACTIVATED)).hasSize(1);
	}
}
