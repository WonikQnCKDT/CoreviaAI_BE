package org.wonik.coreviaai_be.domain.department.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.coreviaai_be.domain.department.service.DepartmentQueryService;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.support.ApiIntegrationTestSupport;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * 일반 사용자용 부서 목록(GET /api/departments)을 실제 보안 필터와 함께 HTTP 경계에서 검증한다.
 * 활성 부서만, 이름·ID 오름차순, 필드 3개, 상한, 접근 대상(프로젝트 API와 같음).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DepartmentApiIntegrationTest extends ApiIntegrationTestSupport {

	private static final String DEPARTMENTS = DepartmentController.PATH;

	@Test
	@DisplayName("활성 부서만 이름 오름차순(같은 이름은 ID 오름차순)으로 id·parentId·name만 준다")
	void listsActiveDepartments() throws Exception {
		final Department root = saveDepartment("본부", null);
		final Department teamA = saveDepartment("가팀", root.getId());
		final Department other = saveDepartment("다른본부", null);
		final Department sameName = saveDepartment("가팀", other.getId());
		final Department inactive = saveDepartment("나팀", root.getId());
		inactive.deactivate();
		departmentRepository.save(inactive);
		saveUser("user01", USER_PASSWORD, SystemRole.USER, teamA.getId());
		final Cookie cookie = loginOk("user01", USER_PASSWORD);

		final JsonNode response = readResponse(getAs(cookie, DEPARTMENTS));
		final List<Long> ids = new ArrayList<>();
		response.forEach(node -> ids.add(node.get("id").asLong()));
		assertThat(ids).containsExactly(teamA.getId(), sameName.getId(), other.getId(), root.getId());

		getAs(cookie, DEPARTMENTS)
			.andExpect(jsonPath("$.response[0].name").value("가팀"))
			.andExpect(jsonPath("$.response[0].parentId").value(root.getId()))
			.andExpect(jsonPath("$.response[0].length()").value(3))
			.andExpect(jsonPath("$.response[0].status").doesNotExist())
			.andExpect(jsonPath("$.response[2].parentId").doesNotExist());
	}

	@Test
	@DisplayName("활성 부서가 없으면 빈 목록")
	void emptyWhenNoActiveDepartment() throws Exception {
		saveUser("user01", USER_PASSWORD, SystemRole.USER, null);
		final Cookie cookie = loginOk("user01", USER_PASSWORD);

		getAs(cookie, DEPARTMENTS)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.length()").value(0));
	}

	@Test
	@DisplayName("부서는 최대 1000개까지만 준다")
	void limitsToMax() throws Exception {
		final int max = DepartmentQueryService.MAX_DEPARTMENTS;
		final List<Object[]> rows = new ArrayList<>();
		for (int i = 0; i <= max; i++) {
			rows.add(new Object[] {String.format("부서%04d", i)});
		}
		jdbcTemplate.batchUpdate(
			"INSERT INTO departments (name, status, version, created_at, updated_at) "
				+ "VALUES (?, 'ACTIVE', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
			rows
		);
		saveUser("user01", USER_PASSWORD, SystemRole.USER, null);
		final Cookie cookie = loginOk("user01", USER_PASSWORD);

		final JsonNode response = readResponse(getAs(cookie, DEPARTMENTS));
		assertThat(response.size()).isEqualTo(max);
		assertThat(response.get(0).get("name").asString()).isEqualTo("부서0000");
		assertThat(response.get(max - 1).get("name").asString()).isEqualTo("부서0999");
	}

	@Test
	@DisplayName("비로그인은 401, 시스템 관리자는 404로 막고, 경영진은 조회할 수 있다")
	void accessControl() throws Exception {
		final Department team = saveDepartment("개발팀", null);
		expectError(getAs(null, DEPARTMENTS), ErrorCode.UNAUTHORIZED);

		saveUser("admin01", USER_PASSWORD, SystemRole.SYSTEM_ADMIN, null);
		expectError(getAs(loginOk("admin01", USER_PASSWORD), DEPARTMENTS), ErrorCode.NOT_FOUND);

		saveUser("exec01", USER_PASSWORD, SystemRole.EXECUTIVE, team.getId());
		getAs(loginOk("exec01", USER_PASSWORD), DEPARTMENTS)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.length()").value(1));
	}
}
