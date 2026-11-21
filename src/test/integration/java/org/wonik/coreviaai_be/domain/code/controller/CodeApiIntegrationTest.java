package org.wonik.coreviaai_be.domain.code.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.support.ApiIntegrationTestSupport;

import jakarta.servlet.http.Cookie;

/**
 * FR-ADM-07 화면용 공통 코드 조회(GET /api/codes/{groupCode})를 HTTP 경계에서 검증한다.
 * 로그인 사용자 누구나, 사용 중 코드만 정렬 순서대로, 관리자 변경이 재배포 없이 바로 반영.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CodeApiIntegrationTest extends ApiIntegrationTestSupport {

	private static final String CODES = "/api/codes/{groupCode}";
	private static final String ADMIN_CODES = "/api/admin/code-groups/{groupCode}/codes";

	private Cookie userCookie;
	private Cookie adminCookie;

	@BeforeEach
	void login() throws Exception {
		final Department team = saveDepartment("개발팀", null);
		saveUser("user01", USER_PASSWORD, SystemRole.USER, team.getId());
		saveUser("admin01", USER_PASSWORD, SystemRole.SYSTEM_ADMIN, null);
		userCookie = loginOk("user01", USER_PASSWORD);
		adminCookie = loginOk("admin01", USER_PASSWORD);
	}

	@Test
	@DisplayName("일반 사용자는 그룹별 사용 중 코드를 정렬 순서대로 라벨 키와 함께 받는다")
	void userReadsSeededCodes() throws Exception {
		getAs(userCookie, CODES, "TASK_STATUS")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.groupCode").value("TASK_STATUS"))
			.andExpect(jsonPath("$.response.labelKey").value("codeGroup.TASK_STATUS"))
			.andExpect(jsonPath("$.response.codes.length()").value(4))
			.andExpect(jsonPath("$.response.codes[0].code").value("TODO"))
			.andExpect(jsonPath("$.response.codes[0].labelKey").value("code.TASK_STATUS.TODO"))
			.andExpect(jsonPath("$.response.codes[0].sortOrder").value(1))
			.andExpect(jsonPath("$.response.codes[3].code").value("DONE"))
			.andExpect(jsonPath("$.response.codes[0].id").doesNotExist())
			.andExpect(jsonPath("$.response.codes[0].active").doesNotExist());
	}

	@Test
	@DisplayName("관리자가 정렬·사용 여부를 바꾸면 다음 조회에 바로 반영되고, 사용 안 함 코드는 빠진다")
	void adminChangesAreReflectedImmediately() throws Exception {
		saveTestCodeGroup("TEST_FLOW");
		postJson("/api/admin/code-groups/TEST_FLOW/codes", adminCookie, body("code", "A", "sortOrder", 1)).andExpect(status().isOk());
		postJson("/api/admin/code-groups/TEST_FLOW/codes", adminCookie, body("code", "B", "sortOrder", 2)).andExpect(status().isOk());
		postJson("/api/admin/code-groups/TEST_FLOW/codes", adminCookie, body("code", "C", "sortOrder", 3)).andExpect(status().isOk());

		patchJson(adminCookie, body("sortOrder", 0, "active", true), ADMIN_CODES + "/{code}", "TEST_FLOW", "C")
			.andExpect(status().isOk());
		patchJson(adminCookie, body("sortOrder", 2, "active", false), ADMIN_CODES + "/{code}", "TEST_FLOW", "B")
			.andExpect(status().isOk());

		getAs(userCookie, CODES, "TEST_FLOW")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.codes.length()").value(2))
			.andExpect(jsonPath("$.response.codes[0].code").value("C"))
			.andExpect(jsonPath("$.response.codes[1].code").value("A"));

		// 관리 화면에서는 사용 안 함 코드도 보인다
		getAs(adminCookie, ADMIN_CODES, "TEST_FLOW")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.length()").value(3));
	}

	@Test
	@DisplayName("코드가 없는 그룹은 빈 목록, 없는 그룹은 404, 형식이 틀린 그룹 코드는 400, 비로그인은 401")
	void errorsAndBoundaries() throws Exception {
		saveTestCodeGroup("TEST_EMPTY");
		getAs(userCookie, CODES, "TEST_EMPTY")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.codes.length()").value(0));

		expectError(getAs(userCookie, CODES, "NO_SUCH_GROUP"), ErrorCode.CODE_GROUP_NOT_FOUND);
		getAs(userCookie, CODES, "task_status").andExpect(status().isBadRequest());
		expectError(getAs(null, CODES, "TASK_STATUS"), ErrorCode.UNAUTHORIZED);
	}

	@Test
	@DisplayName("시스템 관리자도 화면용 코드 조회를 쓸 수 있다(프로젝트 데이터가 아님)")
	void adminCanReadCodes() throws Exception {
		getAs(adminCookie, CODES, "RISK_LEVEL")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.codes.length()").value(3));
	}
}
