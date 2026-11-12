package org.wonik.coreviaai_be.domain.admin.controller;

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
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.department.entity.Department;
import org.wonik.global.user.entity.User;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * FR-ADM-02 부서(조직) 관리를 HTTP 경계에서 검증한다.
 * 계층(parentId), 같은 상위 안 이름 중복, 순환 이동 금지, 사용 중인 부서 비활성화 금지, 감사 로그.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDepartmentApiIntegrationTest extends AdminApiTestSupport {

	private static final String DEPARTMENTS = "/api/admin/departments";

	@Test
	@DisplayName("일반 사용자가 부서 관리 API를 호출하면 404")
	void nonAdminGetsNotFound() throws Exception {
		final Department team = saveDepartment("개발팀", null);
		saveUser("user01", USER_PASSWORD, SystemRole.USER, team.getId());
		final Cookie user = loginOk("user01", USER_PASSWORD);

		expectError(getAs(user, DEPARTMENTS), ErrorCode.NOT_FOUND);
	}

	@Test
	@DisplayName("상위·하위 부서를 만들면 목록에 parentId와 소속 활성 인원 수가 나오고 생성 이력이 남는다")
	void createHierarchy() throws Exception {
		final JsonNode division = readResponse(postJson(DEPARTMENTS, adminCookie, body("name", " IT사업부 ", "parentId", null)));
		final long divisionId = division.get("id").asLong();
		assertThat(division.get("name").asString()).isEqualTo("IT사업부");

		final JsonNode team = readResponse(postJson(DEPARTMENTS, adminCookie, body("name", "개발팀", "parentId", divisionId)));
		saveUser("user01", USER_PASSWORD, SystemRole.USER, team.get("id").asLong());

		getAs(adminCookie, DEPARTMENTS)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.items.length()").value(2))
			.andExpect(jsonPath("$.response.items[0].name").value("IT사업부"))
			.andExpect(jsonPath("$.response.items[0].parentId").doesNotExist())
			.andExpect(jsonPath("$.response.items[0].activeMemberCount").value(0))
			.andExpect(jsonPath("$.response.items[1].parentId").value(divisionId))
			.andExpect(jsonPath("$.response.items[1].activeMemberCount").value(1))
			.andExpect(jsonPath("$.response.hasNext").value(false));

		assertThat(auditLogs(AuditAction.DEPARTMENT_CREATED))
			.hasSize(2)
			.allSatisfy(log -> {
				assertThat(log.getTargetType()).isEqualTo(AuditTargetType.DEPARTMENT);
				assertThat(log.getActorUserId()).isEqualTo(adminId);
			});
	}

	@Test
	@DisplayName("같은 상위 부서 안(최상위 포함)에서는 이름이 겹칠 수 없고, 상위가 다르면 같은 이름을 쓸 수 있다")
	void nameUniqueWithinParent() throws Exception {
		final Department divisionA = saveDepartment("A사업부", null);
		final Department divisionB = saveDepartment("B사업부", null);
		saveDepartment("개발팀", divisionA.getId());

		expectError(
			postJson(DEPARTMENTS, adminCookie, body("name", "개발팀", "parentId", divisionA.getId())),
			ErrorCode.DUPLICATE_DEPARTMENT_NAME
		);
		expectError(
			postJson(DEPARTMENTS, adminCookie, body("name", "A사업부", "parentId", null)),
			ErrorCode.DUPLICATE_DEPARTMENT_NAME
		);
		postJson(DEPARTMENTS, adminCookie, body("name", "개발팀", "parentId", divisionB.getId()))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("상위 부서는 활성 부서여야 하고 없는 부서면 400")
	void parentMustBeActive() throws Exception {
		final Department closed = saveDepartment("폐지사업부", null);
		closed.deactivate();
		departmentRepository.save(closed);

		expectError(
			postJson(DEPARTMENTS, adminCookie, body("name", "개발팀", "parentId", closed.getId())),
			ErrorCode.INVALID_DEPARTMENT_PARENT
		);
		expectError(
			postJson(DEPARTMENTS, adminCookie, body("name", "개발팀", "parentId", 999_999L)),
			ErrorCode.INVALID_DEPARTMENT_PARENT
		);
	}

	@Test
	@DisplayName("자기 자신이나 자기 하위 부서 밑으로 옮길 수 없다(순환 금지)")
	void cannotCreateCycle() throws Exception {
		final Department division = saveDepartment("IT사업부", null);
		final Department team = saveDepartment("개발팀", division.getId());
		final Department part = saveDepartment("백엔드파트", team.getId());

		expectError(
			patchJson(adminCookie, body("name", "IT사업부", "parentId", part.getId()), DEPARTMENTS + "/{id}", division.getId()),
			ErrorCode.INVALID_DEPARTMENT_PARENT
		);
		expectError(
			patchJson(adminCookie, body("name", "IT사업부", "parentId", division.getId()), DEPARTMENTS + "/{id}", division.getId()),
			ErrorCode.INVALID_DEPARTMENT_PARENT
		);

		// 정상 이동: 파트를 사업부 바로 밑으로
		patchJson(adminCookie, body("name", "백엔드파트", "parentId", division.getId()), DEPARTMENTS + "/{id}", part.getId())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.parentId").value(division.getId()));
		final JsonNode details = jsonMapper.readTree(auditLogs(AuditAction.DEPARTMENT_CHANGED).get(0).getDetails());
		assertThat(details.get("parentId").get("before").asLong()).isEqualTo(team.getId());
		assertThat(details.has("name")).isFalse();
	}

	@Test
	@DisplayName("활성 소속 인원이나 활성 하위 부서가 있으면 비활성화할 수 없다")
	void cannotDeactivateDepartmentInUse() throws Exception {
		final Department division = saveDepartment("IT사업부", null);
		final Department team = saveDepartment("개발팀", division.getId());
		final User user = saveUser("user01", USER_PASSWORD, SystemRole.USER, team.getId());

		expectError(
			patchJson(adminCookie, body("status", "INACTIVE"), DEPARTMENTS + "/{id}/status", division.getId()),
			ErrorCode.DEPARTMENT_IN_USE
		);
		expectError(
			patchJson(adminCookie, body("status", "INACTIVE"), DEPARTMENTS + "/{id}/status", team.getId()),
			ErrorCode.DEPARTMENT_IN_USE
		);

		user.deactivate();
		userRepository.save(user);
		patchJson(adminCookie, body("status", "INACTIVE"), DEPARTMENTS + "/{id}/status", team.getId())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.status").value("INACTIVE"));
		patchJson(adminCookie, body("status", "INACTIVE"), DEPARTMENTS + "/{id}/status", division.getId())
			.andExpect(status().isOk());
		assertThat(auditLogs(AuditAction.DEPARTMENT_DEACTIVATED)).hasSize(2);

		// 비활성 상위 밑의 부서는 상위를 먼저 활성화해야 다시 활성화할 수 있다
		expectError(
			patchJson(adminCookie, body("status", "ACTIVE"), DEPARTMENTS + "/{id}/status", team.getId()),
			ErrorCode.INVALID_DEPARTMENT_PARENT
		);
		patchJson(adminCookie, body("status", "ACTIVE"), DEPARTMENTS + "/{id}/status", division.getId())
			.andExpect(status().isOk());
		patchJson(adminCookie, body("status", "ACTIVE"), DEPARTMENTS + "/{id}/status", team.getId())
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("목록은 id 오름차순 커서 페이지로 조회하고, 상태로 거를 수 있다")
	void listWithCursor() throws Exception {
		final Department first = saveDepartment("A사업부", null);
		final Department second = saveDepartment("B사업부", null);
		final Department third = saveDepartment("C사업부", null);
		third.deactivate();
		departmentRepository.save(third);

		final JsonNode page1 = readResponse(getAs(adminCookie, DEPARTMENTS + "?size=2"));
		assertThat(ids(page1)).containsExactly(first.getId(), second.getId());
		assertThat(page1.get("hasNext").asBoolean()).isTrue();

		final JsonNode page2 = readResponse(
			getAs(adminCookie, DEPARTMENTS + "?size=2&cursor=" + page1.get("nextCursor").asString())
		);
		assertThat(ids(page2)).containsExactly(third.getId());
		assertThat(page2.get("hasNext").asBoolean()).isFalse();

		// 마지막 페이지가 size와 정확히 같으면 다음 페이지가 없다고 판단한다(1개를 더 읽어 확인)
		final JsonNode exact = readResponse(getAs(adminCookie, DEPARTMENTS + "?size=3"));
		assertThat(ids(exact)).hasSize(3);
		assertThat(exact.get("hasNext").asBoolean()).isFalse();

		assertThat(ids(readResponse(getAs(adminCookie, DEPARTMENTS + "?status=INACTIVE")))).containsExactly(third.getId());
	}

	@Test
	@DisplayName("잘못된 커서는 400 INVALID_CURSOR, 범위를 벗어난 size는 400")
	void invalidCursorOrSize() throws Exception {
		expectError(getAs(adminCookie, DEPARTMENTS + "?cursor=not-a-cursor"), ErrorCode.INVALID_CURSOR);
		getAs(adminCookie, DEPARTMENTS + "?size=0").andExpect(status().isBadRequest());
		getAs(adminCookie, DEPARTMENTS + "?size=101").andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("같은 부서 추가 요청을 다시 보내면 409로 끝나고 부서는 하나만 생긴다")
	void resentCreateRequestIsRejected() throws Exception {
		postJson(DEPARTMENTS, adminCookie, body("name", "개발팀", "parentId", null)).andExpect(status().isOk());
		expectError(
			postJson(DEPARTMENTS, adminCookie, body("name", "개발팀", "parentId", null)),
			ErrorCode.DUPLICATE_DEPARTMENT_NAME
		);
		assertThat(departmentRepository.findAll()).hasSize(1);
	}

	@Test
	@DisplayName("없는 부서를 수정하면 404 DEPARTMENT_NOT_FOUND")
	void unknownDepartment() throws Exception {
		expectError(
			patchJson(adminCookie, body("name", "이름", "parentId", null), DEPARTMENTS + "/{id}", 999_999L),
			ErrorCode.DEPARTMENT_NOT_FOUND
		);
	}

	private static List<Long> ids(final JsonNode page) {
		final List<Long> ids = new ArrayList<>();
		page.get("items").forEach(item -> ids.add(item.get("id").asLong()));
		return ids;
	}
}
