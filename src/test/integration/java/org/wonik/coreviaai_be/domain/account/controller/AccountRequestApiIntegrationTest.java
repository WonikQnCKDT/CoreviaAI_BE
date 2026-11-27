package org.wonik.coreviaai_be.domain.account.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.account.entity.AccountRequestStatus;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditLog;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.UserStatus;
import org.wonik.support.ApiIntegrationTestSupport;
import org.wonik.support.TestClockConfig;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * FR-ADM-10 계정 신청·승인. 승인 전 로그인 불가, 반려 사유, 신청자 상태 조회, 처리 이력.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountRequestApiIntegrationTest extends ApiIntegrationTestSupport {

	private static final String APPLY = AccountRequestController.PATH;
	private static final String ADMIN = "/api/admin/account-requests";
	private static final String ADMIN_LOGIN_ID = "admin01";
	private static final String ADMIN_PASSWORD = "Adm1n-pass!";

	private Department devTeam;
	private Long adminId;
	private Cookie adminCookie;

	@BeforeEach
	void setUpAdminAndDepartment() throws Exception {
		devTeam = saveDepartment("개발팀", null);
		adminId = saveUser(ADMIN_LOGIN_ID, ADMIN_PASSWORD, SystemRole.SYSTEM_ADMIN, null).getId();
		adminCookie = loginOk(ADMIN_LOGIN_ID, ADMIN_PASSWORD);
	}

	@Test
	@DisplayName("신청은 로그인 없이 받고, 승인 전에는 로그인되지 않으며 이름까지 맞아야 상태를 본다")
	void applicantCanSubmitAndSeeOwnStatusButCannotLogin() throws Exception {
		final JsonNode created = readResponse(postJson(APPLY, null, applyBody("hong.gd", "홍길동", devTeam.getId())));

		assertThat(created.get("loginId").asString()).isEqualTo("hong.gd");
		assertThat(created.get("status").asString()).isEqualTo("PENDING");
		assertThat(created.has("purpose")).isFalse();
		expectError(login("hong.gd", "anything1!"), ErrorCode.INVALID_CREDENTIALS);

		final JsonNode status = readResponse(mockMvc.perform(get(AccountRequestController.STATUS_PATH)
			.param("loginId", "hong.gd")
			.param("name", " 홍길동 ")));
		assertThat(status.get("status").asString()).isEqualTo("PENDING");
		assertThat(status.get("rejectReason").isNull()).isTrue();
		expectError(
			mockMvc.perform(get(AccountRequestController.STATUS_PATH).param("loginId", "hong.gd").param("name", "다른사람")),
			ErrorCode.ACCOUNT_REQUEST_NOT_FOUND
		);

		final List<AuditLog> submitted = auditLogs(AuditAction.ACCOUNT_REQUEST_SUBMITTED);
		assertThat(submitted).hasSize(1);
		assertThat(submitted.get(0).getActorUserId()).isNull();
		assertThat(submitted.get(0).getTargetType()).isEqualTo(AuditTargetType.ACCOUNT_REQUEST);
		assertThat(submitted.get(0).getIpAddress()).isNotBlank();
		assertThat(submitted.get(0).getOccurredAt()).isEqualTo(TestClockConfig.START);
		assertThat(submitted.get(0).getDetails()).doesNotContain("password");
	}

	@Test
	@DisplayName("같은 아이디의 대기 신청과 이미 있는 계정은 다시 신청할 수 없다")
	void duplicatePendingAndExistingUserAreRejected() throws Exception {
		final Map<String, Object> body = applyBody("hong.gd", "홍길동", devTeam.getId());
		postJson(APPLY, null, body).andExpect(status().isOk());
		expectError(postJson(APPLY, null, body), ErrorCode.DUPLICATE_ACCOUNT_REQUEST);

		saveUser("taken.id", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		expectError(postJson(APPLY, null, applyBody("taken.id", "기존사용자", devTeam.getId())), ErrorCode.DUPLICATE_LOGIN_ID);
	}

	@Test
	@DisplayName("빈 사용 목적·없는 부서·비활성 부서는 신청이 거절된다")
	void invalidSubmitIsRejected() throws Exception {
		expectValidationError(
			postJson(APPLY, null, body("loginId", "hong.gd", "name", "홍길동", "purpose", "  ")),
			"사용 목적을 입력해 주세요"
		);
		expectError(
			postJson(APPLY, null, applyBody("hong.gd", "홍길동", 999_999L)),
			ErrorCode.DEPARTMENT_NOT_FOUND
		);
		final Department inactive = saveDepartment("휴직", null);
		inactive.deactivate();
		departmentRepository.save(inactive);
		expectError(
			postJson(APPLY, null, applyBody("hong.gd", "홍길동", inactive.getId())),
			ErrorCode.DEPARTMENT_INACTIVE
		);
	}

	@Test
	@DisplayName("일반 사용자와 비로그인 사용자는 승인 API를 쓰지 못한다")
	void reviewRequiresSystemAdmin() throws Exception {
		saveUser("user01", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		final Cookie user = loginOk("user01", USER_PASSWORD);
		expectError(getAs(user, ADMIN), ErrorCode.NOT_FOUND);
		expectError(getAs(null, ADMIN), ErrorCode.UNAUTHORIZED);
	}

	@Test
	@DisplayName("반려는 사유가 필수이고 신청자는 사유와 처리 시각을 조회한다")
	void rejectRequiresReasonAndApplicantSeesIt() throws Exception {
		final long requestId = submit("hong.gd", "홍길동");
		postJson(ADMIN + "/" + requestId + "/reject", adminCookie, body("version", 0, "rejectReason", " "))
			.andExpect(status().isBadRequest());

		final JsonNode rejected = readResponse(postJson(
			ADMIN + "/" + requestId + "/reject",
			adminCookie,
			body("version", 0, "rejectReason", " 소속 확인 불가 ")
		));
		assertThat(rejected.get("status").asString()).isEqualTo("REJECTED");
		assertThat(rejected.get("rejectReason").asString()).isEqualTo("소속 확인 불가");
		assertThat(rejected.get("reviewedBy").asLong()).isEqualTo(adminId);
		assertThat(rejected.get("reviewerName").asString()).contains("admin01");
		assertThat(rejected.get("reviewedAt").asString()).isEqualTo(TestClockConfig.START.toString());

		final JsonNode status = readResponse(mockMvc.perform(get(AccountRequestController.STATUS_PATH)
			.param("loginId", "hong.gd")
			.param("name", "홍길동")));
		assertThat(status.get("status").asString()).isEqualTo("REJECTED");
		assertThat(status.get("rejectReason").asString()).isEqualTo("소속 확인 불가");
		expectError(login("hong.gd", "anything1!"), ErrorCode.INVALID_CREDENTIALS);

		final AuditLog log = auditLogs(AuditAction.ACCOUNT_REQUEST_REJECTED).get(0);
		assertThat(log.getActorUserId()).isEqualTo(adminId);
		assertThat(log.getTargetId()).isEqualTo(requestId);
		assertThat(log.getDetails()).contains("소속 확인 불가");

		final JsonNode again = readResponse(postJson(APPLY, null, applyBody("hong.gd", "홍길동", devTeam.getId())));
		assertThat(again.get("id").asLong()).isNotEqualTo(requestId);
		assertThat(again.get("status").asString()).isEqualTo("PENDING");
	}

	@Test
	@DisplayName("승인은 역할·소속을 정해 활성 사용자를 만들고, 비밀번호 초기화 뒤에만 로그인된다")
	void approveCreatesActiveUserAndBlocksLoginUntilPasswordReset() throws Exception {
		final long requestId = submit("hong.gd", " 홍길동 ");

		expectError(
			postJson(ADMIN + "/" + requestId + "/approve", adminCookie, body(
				"version", 0,
				"systemRole", "USER"
			)),
			ErrorCode.DEPARTMENT_REQUIRED
		);

		final JsonNode approved = readResponse(postJson(ADMIN + "/" + requestId + "/approve", adminCookie, body(
			"version", 0,
			"systemRole", "USER",
			"departmentId", devTeam.getId(),
			"jobTitle", "백엔드"
		)));
		assertThat(approved.get("status").asString()).isEqualTo("APPROVED");
		assertThat(approved.get("name").asString()).isEqualTo("홍길동");
		assertThat(approved.get("departmentName").asString()).isEqualTo("개발팀");
		assertThat(approved.get("userId").isNull()).isFalse();
		final long userId = approved.get("userId").asLong();
		assertThat(findUser(userId).getStatus()).isEqualTo(UserStatus.ACTIVE);
		assertThat(findUser(userId).getSystemRole()).isEqualTo(SystemRole.USER);
		assertThat(findUser(userId).getJobTitle()).isEqualTo("백엔드");

		expectError(login("hong.gd", "anything1!"), ErrorCode.INVALID_CREDENTIALS);
		final JsonNode reset = readResponse(
			mockMvc.perform(post("/api/admin/users/{id}/password-reset", userId).cookie(adminCookie))
		);
		login("hong.gd", reset.get("temporaryPassword").asString()).andExpect(status().isOk());

		assertThat(auditLogs(AuditAction.ACCOUNT_REQUEST_APPROVED)).hasSize(1);
		assertThat(auditLogs(AuditAction.USER_CREATED)).hasSize(1);
		assertThat(auditLogs(AuditAction.USER_ACTIVATED)).hasSize(1);
		assertThat(auditLogs(AuditAction.USER_CREATED).get(0).getActorUserId()).isEqualTo(adminId);
		assertThat(auditLogs(AuditAction.USER_ACTIVATED).get(0).getTargetId()).isEqualTo(userId);
		assertThat(auditLogs(AuditAction.ACCOUNT_REQUEST_APPROVED).get(0).getDetails()).doesNotContain("password");

		expectError(
			postJson(ADMIN + "/" + requestId + "/reject", adminCookie, body("version", 1, "rejectReason", "늦음")),
			ErrorCode.ACCOUNT_REQUEST_ALREADY_REVIEWED
		);
	}

	@Test
	@DisplayName("다른 버전의 승인은 거절하고 대기 목록은 커서로 이어서 본다")
	void versionConflictAndCursorList() throws Exception {
		final long first = submit("first.id", "첫번째");
		final long second = submit("second.id", "두번째");

		expectError(
			postJson(ADMIN + "/" + first + "/approve", adminCookie, body(
				"version", 9,
				"systemRole", "SYSTEM_ADMIN"
			)),
			ErrorCode.CONCURRENT_MODIFICATION
		);

		final JsonNode page = readResponse(getAs(adminCookie, ADMIN + "?status=PENDING&size=1"));
		assertThat(page.get("items")).hasSize(1);
		assertThat(page.get("items").get(0).get("id").asLong()).isEqualTo(first);
		assertThat(page.get("hasNext").asBoolean()).isTrue();

		final String cursor = page.get("nextCursor").asString();
		final JsonNode next = readResponse(getAs(adminCookie, ADMIN + "?status=PENDING&size=1&cursor=" + cursor));
		assertThat(next.get("items")).hasSize(1);
		assertThat(next.get("items").get(0).get("id").asLong()).isEqualTo(second);
		assertThat(next.get("hasNext").asBoolean()).isFalse();
		assertThat(next.get("items").get(0).get("status").asString()).isEqualTo(AccountRequestStatus.PENDING.name());
	}

	private long submit(final String loginId, final String name) throws Exception {
		return readResponse(postJson(APPLY, null, applyBody(loginId, name, null))).get("id").asLong();
	}

	private Map<String, Object> applyBody(final String loginId, final String name, final Long departmentId) {
		return body("loginId", loginId, "name", name, "departmentId", departmentId, "purpose", "프로젝트 협업");
	}

	private void expectValidationError(
		final org.springframework.test.web.servlet.ResultActions actions,
		final String message
	) throws Exception {
		actions.andExpect(status().isBadRequest())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.success").value(false))
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.error.message").value(message));
	}
}
