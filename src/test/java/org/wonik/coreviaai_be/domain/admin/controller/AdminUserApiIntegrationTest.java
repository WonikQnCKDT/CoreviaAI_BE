package org.wonik.coreviaai_be.domain.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditLog;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.department.entity.Department;
import org.wonik.global.user.entity.User;
import org.wonik.support.TestClockConfig;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * FR-ADM-02 사용자 관리 인수 기준을 HTTP 경계에서 검증한다.
 * 사용자 추가·비활성화, 부서·역할 변경, 비활성 사용자 로그인 차단, 프로젝트 생성 권한, 변경 이력(감사 로그), 커서 목록.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminUserApiIntegrationTest extends AdminApiTestSupport {

	private static final String USERS = "/api/admin/users";

	private Department devTeam;

	@BeforeEach
	void setUpDepartment() {
		devTeam = saveDepartment("개발팀", null);
	}

	// ---- 권한 ----

	@Test
	@DisplayName("일반 사용자가 관리자 API를 호출하면 존재를 숨기고 404")
	void nonAdminGetsNotFound() throws Exception {
		saveUser("user01", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		final Cookie user = loginOk("user01", USER_PASSWORD);

		expectError(getAs(user, USERS), ErrorCode.NOT_FOUND);
		expectError(postJson(USERS, user, createBody("user02", SystemRole.USER, devTeam.getId())), ErrorCode.NOT_FOUND);
	}

	// ---- 사용자 추가 ----

	@Test
	@DisplayName("사용자 추가 후 관리자 비밀번호 초기화로 발급한 임시 비밀번호로 로그인할 수 있다")
	void createUserThenIssuePassword() throws Exception {
		final JsonNode created = readResponse(postJson(USERS, adminCookie, body(
			"loginId", "hong.gd",
			"name", " 홍길동 ",
			"systemRole", "USER",
			"departmentId", devTeam.getId(),
			"jobTitle", "백엔드 개발",
			"canCreateProject", true
		)));

		assertThat(created.get("loginId").asString()).isEqualTo("hong.gd");
		assertThat(created.get("name").asString()).isEqualTo("홍길동");
		assertThat(created.get("status").asString()).isEqualTo("ACTIVE");
		assertThat(created.get("departmentName").asString()).isEqualTo("개발팀");
		assertThat(created.get("canCreateProject").asBoolean()).isTrue();

		// 비밀번호가 아직 없으므로 로그인 불가
		expectError(login("hong.gd", "anything1!"), ErrorCode.INVALID_CREDENTIALS);

		final long userId = created.get("id").asLong();
		final JsonNode reset = readResponse(
			mockMvc.perform(post("/api/admin/users/{id}/password-reset", userId).cookie(adminCookie))
		);
		login("hong.gd", reset.get("temporaryPassword").asString())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.mustChangePassword").value(true));

		final List<AuditLog> createdLogs = auditLogs(AuditAction.USER_CREATED);
		assertThat(createdLogs).hasSize(1);
		final AuditLog createdLog = createdLogs.get(0);
		assertThat(createdLog.getActorUserId()).isEqualTo(adminId);
		assertThat(createdLog.getTargetType()).isEqualTo(AuditTargetType.USER);
		assertThat(createdLog.getTargetId()).isEqualTo(userId);
		assertThat(createdLog.getIpAddress()).isNotBlank();
		// 기록 시각은 주입된 Clock(UTC instant)을 따른다
		assertThat(createdLog.getOccurredAt()).isEqualTo(TestClockConfig.START);
	}

	@Test
	@DisplayName("같은 추가 요청을 다시 보내면 409 DUPLICATE_LOGIN_ID로 끝나고 계정은 하나만 생긴다")
	void resentCreateRequestIsRejected() throws Exception {
		final Map<String, Object> request = createBody("user01", SystemRole.USER, devTeam.getId());

		postJson(USERS, adminCookie, request).andExpect(status().isOk());
		expectError(postJson(USERS, adminCookie, request), ErrorCode.DUPLICATE_LOGIN_ID);
		assertThat(userRepository.findAll()).filteredOn(user -> user.getLoginId().equals("user01")).hasSize(1);
		assertThat(auditLogs(AuditAction.USER_CREATED)).hasSize(1);
	}

	@Test
	@DisplayName("시스템 관리자가 아니면 부서가 필수이고, 시스템 관리자는 부서 없이 추가할 수 있다")
	void departmentRequiredExceptAdmin() throws Exception {
		expectError(
			postJson(USERS, adminCookie, createBody("user01", SystemRole.USER, null)),
			ErrorCode.DEPARTMENT_REQUIRED
		);
		expectError(
			postJson(USERS, adminCookie, createBody("exec01", SystemRole.EXECUTIVE, null)),
			ErrorCode.DEPARTMENT_REQUIRED
		);
		postJson(USERS, adminCookie, createBody("admin02", SystemRole.SYSTEM_ADMIN, null)).andExpect(status().isOk());
	}

	@Test
	@DisplayName("없는 부서는 404, 비활성 부서에는 배치할 수 없다(400). 실패한 요청은 계정도 이력도 남기지 않는다")
	void departmentMustExistAndBeActive() throws Exception {
		final Department closed = saveDepartment("폐지팀", null);
		closed.deactivate();
		departmentRepository.save(closed);

		expectError(
			postJson(USERS, adminCookie, createBody("user01", SystemRole.USER, 999_999L)),
			ErrorCode.DEPARTMENT_NOT_FOUND
		);
		expectError(
			postJson(USERS, adminCookie, createBody("user01", SystemRole.USER, closed.getId())),
			ErrorCode.DEPARTMENT_INACTIVE
		);
		assertThat(userRepository.existsByLoginId("user01")).isFalse();
		assertThat(auditLogs(AuditAction.USER_CREATED)).isEmpty();
	}

	@Test
	@DisplayName("입력값 형식 오류는 저장하지 않고 400")
	void invalidInput() throws Exception {
		postJson(USERS, adminCookie, createBody("공백 있는 아이디", SystemRole.USER, devTeam.getId()))
			.andExpect(status().isBadRequest());
		postJson(USERS, adminCookie, body("loginId", "user01", "name", "", "systemRole", "USER"))
			.andExpect(status().isBadRequest());
		assertThat(userRepository.existsByLoginId("user01")).isFalse();
	}

	// ---- 비활성화·로그인 차단 ----

	@Test
	@DisplayName("비활성화하면 기존 세션이 끊기고 로그인이 차단되며, 다시 활성화하면 로그인할 수 있다")
	void deactivateBlocksLogin() throws Exception {
		final User user = saveUser("user01", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		final Cookie userCookie = loginOk("user01", USER_PASSWORD);

		patchJson(adminCookie, body("status", "INACTIVE"), USERS + "/{id}/status", user.getId())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.status").value("INACTIVE"));

		expectError(getAs(userCookie, "/api/auth/me"), ErrorCode.SESSION_EXPIRED);
		expectError(login("user01", USER_PASSWORD), ErrorCode.ACCOUNT_DISABLED);
		assertThat(auditLogs(AuditAction.USER_DEACTIVATED)).hasSize(1);

		patchJson(adminCookie, body("status", "ACTIVE"), USERS + "/{id}/status", user.getId())
			.andExpect(status().isOk());
		login("user01", USER_PASSWORD).andExpect(status().isOk());
		assertThat(auditLogs(AuditAction.USER_ACTIVATED)).hasSize(1);
	}

	@Test
	@DisplayName("소속 부서가 비활성이면 다시 활성화할 수 없다")
	void cannotActivateIntoInactiveDepartment() throws Exception {
		final User user = saveUser("user01", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		user.deactivate();
		userRepository.save(user);
		devTeam.deactivate();
		departmentRepository.save(devTeam);

		expectError(
			patchJson(adminCookie, body("status", "ACTIVE"), USERS + "/{id}/status", user.getId()),
			ErrorCode.DEPARTMENT_INACTIVE
		);
	}

	// ---- 역할 변경 ----

	@Test
	@DisplayName("역할을 바꾸면 기존 세션이 끊기고, 일반 사용자가 아니게 되면 프로젝트 생성 권한이 회수된다")
	void changeRoleTerminatesSessionAndRevokesProjectCreation() throws Exception {
		final User user = saveUser("user01", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		patchJson(adminCookie, body("enabled", true), USERS + "/{id}/project-creation", user.getId())
			.andExpect(status().isOk());
		final Cookie userCookie = loginOk("user01", USER_PASSWORD);

		patchJson(adminCookie, body("systemRole", "EXECUTIVE"), USERS + "/{id}/system-role", user.getId())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.systemRole").value("EXECUTIVE"))
			.andExpect(jsonPath("$.response.canCreateProject").value(false));

		expectError(getAs(userCookie, "/api/auth/me"), ErrorCode.SESSION_EXPIRED);
		login("user01", USER_PASSWORD)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.systemRole").value("EXECUTIVE"));

		final AuditLog roleLog = auditLogs(AuditAction.USER_ROLE_CHANGED).get(0);
		final JsonNode details = jsonMapper.readTree(roleLog.getDetails());
		assertThat(details.get("systemRole").get("before").asString()).isEqualTo("USER");
		assertThat(details.get("systemRole").get("after").asString()).isEqualTo("EXECUTIVE");
		assertThat(details.get("canCreateProject").get("after").asBoolean()).isFalse();
	}

	@Test
	@DisplayName("부서가 없는 시스템 관리자는 부서를 먼저 지정해야 다른 역할로 바꿀 수 있다")
	void demotingAdminRequiresDepartment() throws Exception {
		final User admin2 = saveUser("admin02", ADMIN_PASSWORD, SystemRole.SYSTEM_ADMIN, null);

		expectError(
			patchJson(adminCookie, body("systemRole", "USER"), USERS + "/{id}/system-role", admin2.getId()),
			ErrorCode.DEPARTMENT_REQUIRED
		);
	}

	@Test
	@DisplayName("본인 계정의 역할·상태는 바꿀 수 없다")
	void cannotChangeOwnAccount() throws Exception {
		expectError(
			patchJson(adminCookie, body("systemRole", "USER"), USERS + "/{id}/system-role", adminId),
			ErrorCode.CANNOT_CHANGE_OWN_ACCOUNT
		);
		expectError(
			patchJson(adminCookie, body("status", "INACTIVE"), USERS + "/{id}/status", adminId),
			ErrorCode.CANNOT_CHANGE_OWN_ACCOUNT
		);
	}

	@Test
	@DisplayName("다른 활성 관리자가 남지 않으면 관리자를 비활성화·강등할 수 없다(409 LAST_SYSTEM_ADMIN)")
	void lastActiveAdminIsProtected() throws Exception {
		final User admin2 = saveUser("admin02", ADMIN_PASSWORD, SystemRole.SYSTEM_ADMIN, devTeam.getId());
		// 요청한 관리자 계정이 다른 경로로 막 비활성화된 상황(세션은 아직 남음)을 만든다
		final User admin1 = findUser(adminId);
		admin1.deactivate();
		userRepository.save(admin1);

		expectError(
			patchJson(adminCookie, body("status", "INACTIVE"), USERS + "/{id}/status", admin2.getId()),
			ErrorCode.LAST_SYSTEM_ADMIN
		);
		expectError(
			patchJson(adminCookie, body("systemRole", "USER"), USERS + "/{id}/system-role", admin2.getId()),
			ErrorCode.LAST_SYSTEM_ADMIN
		);
		assertThat(findUser(admin2.getId()).isActive()).isTrue();
	}

	// ---- 프로젝트 생성 권한 ----

	@Test
	@DisplayName("프로젝트 생성 권한은 일반 사용자에게만 부여된다")
	void projectCreationOnlyForUser() throws Exception {
		final User user = saveUser("user01", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		final User executive = saveUser("exec01", USER_PASSWORD, SystemRole.EXECUTIVE, devTeam.getId());

		expectError(
			patchJson(adminCookie, body("enabled", true), USERS + "/{id}/project-creation", executive.getId()),
			ErrorCode.PROJECT_CREATION_NOT_ALLOWED
		);
		expectError(
			postJson(USERS, adminCookie, body(
				"loginId", "exec02", "name", "경영진", "systemRole", "EXECUTIVE",
				"departmentId", devTeam.getId(), "canCreateProject", true
			)),
			ErrorCode.PROJECT_CREATION_NOT_ALLOWED
		);

		patchJson(adminCookie, body("enabled", true), USERS + "/{id}/project-creation", user.getId())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.canCreateProject").value(true));
		getAs(adminCookie, USERS + "/{id}", user.getId())
			.andExpect(jsonPath("$.response.canCreateProject").value(true));

		// 같은 값으로 다시 요청하면 이력이 늘지 않는다
		patchJson(adminCookie, body("enabled", true), USERS + "/{id}/project-creation", user.getId())
			.andExpect(status().isOk());
		assertThat(auditLogs(AuditAction.PROJECT_CREATION_GRANTED)).hasSize(1);

		patchJson(adminCookie, body("enabled", false), USERS + "/{id}/project-creation", user.getId())
			.andExpect(jsonPath("$.response.canCreateProject").value(false));
		assertThat(auditLogs(AuditAction.PROJECT_CREATION_REVOKED)).hasSize(1);
	}

	// ---- 프로필 변경·이력 ----

	@Test
	@DisplayName("이름·부서·직무를 바꾸면 변경 전·후 값과 변경자가 이력에 남고, 같은 값이면 이력이 남지 않는다")
	void updateProfileRecordsHistory() throws Exception {
		final Department qaTeam = saveDepartment("품질팀", null);
		final User user = saveUser("user01", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		final Map<String, Object> changeBody = body("name", "새 이름", "departmentId", qaTeam.getId(), "jobTitle", "QA");

		patchJson(adminCookie, changeBody, USERS + "/{id}", user.getId())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.departmentName").value("품질팀"))
			.andExpect(jsonPath("$.response.jobTitle").value("QA"));
		patchJson(adminCookie, changeBody, USERS + "/{id}", user.getId()).andExpect(status().isOk());

		getAs(adminCookie, USERS + "/{id}/histories", user.getId())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.items.length()").value(1))
			.andExpect(jsonPath("$.response.items[0].action").value("USER_PROFILE_CHANGED"))
			.andExpect(jsonPath("$.response.items[0].actorUserId").value(adminId))
			.andExpect(jsonPath("$.response.items[0].actorName").value(ADMIN_ID + " 이름"))
			.andExpect(jsonPath("$.response.items[0].details.departmentId.before").value(devTeam.getId()))
			.andExpect(jsonPath("$.response.items[0].details.departmentId.after").value(qaTeam.getId()))
			.andExpect(jsonPath("$.response.items[0].details.jobTitle.before").doesNotExist())
			.andExpect(jsonPath("$.response.hasNext").value(false));
	}

	@Test
	@DisplayName("일반 사용자의 부서를 비울 수 없다")
	void cannotClearDepartmentOfUser() throws Exception {
		final User user = saveUser("user01", USER_PASSWORD, SystemRole.USER, devTeam.getId());

		expectError(
			patchJson(adminCookie, body("name", "이름", "departmentId", null, "jobTitle", null), USERS + "/{id}", user.getId()),
			ErrorCode.DEPARTMENT_REQUIRED
		);
	}

	@Test
	@DisplayName("없는 사용자는 404 USER_NOT_FOUND")
	void unknownUser() throws Exception {
		expectError(getAs(adminCookie, USERS + "/{id}", 999_999L), ErrorCode.USER_NOT_FOUND);
		expectError(getAs(adminCookie, USERS + "/{id}/histories", 999_999L), ErrorCode.USER_NOT_FOUND);
	}

	// ---- 목록(커서) ----

	@Test
	@DisplayName("목록은 id 내림차순 커서 페이지로 조회하고, nextCursor로 다음 페이지를 이어서 받는다")
	void listWithCursor() throws Exception {
		final Long first = saveUser("user01", USER_PASSWORD, SystemRole.USER, devTeam.getId()).getId();
		final Long second = saveUser("user02", USER_PASSWORD, SystemRole.USER, devTeam.getId()).getId();
		final Long third = saveUser("user03", USER_PASSWORD, SystemRole.USER, devTeam.getId()).getId();

		final JsonNode page1 = readResponse(getAs(adminCookie, USERS + "?size=2&systemRole=USER"));
		assertThat(ids(page1)).containsExactly(third, second);
		assertThat(page1.get("hasNext").asBoolean()).isTrue();

		final String cursor = page1.get("nextCursor").asString();
		final JsonNode page2 = readResponse(getAs(adminCookie, USERS + "?size=2&systemRole=USER&cursor=" + cursor));
		assertThat(ids(page2)).containsExactly(first);
		assertThat(page2.get("hasNext").asBoolean()).isFalse();
		final JsonNode nextCursor = page2.get("nextCursor");
		assertThat(nextCursor == null || nextCursor.isNull()).isTrue();
	}

	@Test
	@DisplayName("부서·상태·키워드(아이디·이름 부분 일치) 조건으로 거른다")
	void listWithFilters() throws Exception {
		final Department qaTeam = saveDepartment("품질팀", null);
		saveUser("dev.kim", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		final User qa = saveUser("qa.lee", USER_PASSWORD, SystemRole.USER, qaTeam.getId());
		final User inactive = saveUser("dev.park", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		inactive.deactivate();
		userRepository.save(inactive);

		assertThat(ids(readResponse(getAs(adminCookie, USERS + "?departmentId=" + qaTeam.getId()))))
			.containsExactly(qa.getId());
		assertThat(ids(readResponse(getAs(adminCookie, USERS + "?status=INACTIVE"))))
			.containsExactly(inactive.getId());
		assertThat(readResponse(getAs(adminCookie, USERS + "?keyword=DEV")).get("items")).hasSize(2);
		// LIKE 특수문자는 문자 그대로 찾는다
		assertThat(readResponse(getAs(adminCookie, USERS + "?keyword=%25")).get("items")).isEmpty();
	}

	@Test
	@DisplayName("잘못된 커서는 400 INVALID_CURSOR, 범위를 벗어난 size는 400")
	void invalidCursorOrSize() throws Exception {
		expectError(getAs(adminCookie, USERS + "?cursor=not-a-cursor"), ErrorCode.INVALID_CURSOR);
		getAs(adminCookie, USERS + "?size=0").andExpect(status().isBadRequest());
		getAs(adminCookie, USERS + "?size=101").andExpect(status().isBadRequest());
		getAs(adminCookie, USERS + "?status=UNKNOWN").andExpect(status().isBadRequest());
	}

	// ---- helpers ----

	private static Map<String, Object> createBody(final String loginId, final SystemRole role, final Long departmentId) {
		return body("loginId", loginId, "name", loginId + " 이름", "systemRole", role.name(), "departmentId", departmentId);
	}

	private static List<Long> ids(final JsonNode page) {
		final List<Long> ids = new ArrayList<>();
		page.get("items").forEach(item -> ids.add(item.get("id").asLong()));
		return ids;
	}
}
