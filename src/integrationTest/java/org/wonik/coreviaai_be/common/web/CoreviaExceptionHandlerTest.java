package org.wonik.coreviaai_be.common.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.wonik.coreviaai_be.domain.admin.service.AdminUserService;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.service.ProjectMemberService;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.support.ApiIntegrationTestSupport;

import jakarta.servlet.http.Cookie;

/**
 * 업무 모듈 전체의 동시 수정 충돌 응답(409 CONCURRENT_MODIFICATION). 관리자 API(기존)와 프로젝트 API(FR-PRJ-04) 모두
 * 같은 처리기를 거치는지 본다. 충돌은 저장 시점의 낙관적 잠금 예외로 결정적으로 재현한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CoreviaExceptionHandlerTest extends ApiIntegrationTestSupport {

	@MockitoSpyBean
	private ProjectMemberService projectMemberService;
	@MockitoSpyBean
	private AdminUserService adminUserService;

	@Test
	@DisplayName("프로젝트 멤버 역할 변경이 다른 요청과 겹치면 500이 아니라 409")
	void projectApiConflict() throws Exception {
		saveUser("user01", USER_PASSWORD, SystemRole.USER, null);
		final Cookie cookie = loginOk("user01", USER_PASSWORD);
		doThrow(new ObjectOptimisticLockingFailureException(ProjectMember.class, 1L))
			.when(projectMemberService).changeRole(any(), anyLong(), anyLong(), any());

		expectError(patchJson(cookie, body("actor", "DEVELOPER"), "/api/projects/{id}/members/{userId}/role", 1L, 2L),
			ErrorCode.CONCURRENT_MODIFICATION);
	}

	@Test
	@DisplayName("관리자 API도 같은 처리기로 409를 받는다(관리자 전용 처리기를 합친 뒤에도 유지)")
	void adminApiConflict() throws Exception {
		saveUser("admin01", USER_PASSWORD, SystemRole.SYSTEM_ADMIN, null);
		final Cookie cookie = loginOk("admin01", USER_PASSWORD);
		doThrow(new ObjectOptimisticLockingFailureException(User.class, 1L))
			.when(adminUserService).changeStatus(any(), anyLong(), any());

		expectError(patchJson(cookie, body("status", "INACTIVE"), "/api/admin/users/{id}/status", 1L),
			ErrorCode.CONCURRENT_MODIFICATION);
	}
}
