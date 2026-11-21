package org.wonik.coreviaai_be.domain.project.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * FR-PRJ-04 시스템 관리자의 PM 재지정(요구사항 명세서 3.1 예외: 활성 PM이 남지 않은 경우)을 HTTP·DB 경계에서 검증한다.
 * 관리자는 프로젝트 데이터를 볼 수 없으므로 코드·새 PM ID만 보내고, 결과 확인은 프로젝트 멤버(developer) 계정으로 한다.
 * 프로젝트 API 준비(부서·사용자·프로젝트)를 그대로 쓰려고 프로젝트 테스트 패키지에 둔다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminProjectPmApiIntegrationTest extends ProjectApiTestSupport {

	private static final String REASSIGN = "/api/admin/projects/pm";

	private long projectId;
	private User admin;
	private Cookie adminCookie;

	@BeforeEach
	void createProjectAndAdmin() throws Exception {
		projectId = createProject(validCreateBody("PUR-2026"));
		admin = saveUser("admin01", USER_PASSWORD, SystemRole.SYSTEM_ADMIN, null);
		adminCookie = loginAs(admin);
	}

	@Test
	@DisplayName("활성 PM이 없으면 기존 멤버를 PM으로 바꾸고 비활성 PM을 멤버에서 뺀다. 이력·감사 로그의 처리자는 관리자다")
	void reassignToExistingMember() throws Exception {
		deactivate(creator);

		final JsonNode result = readResponse(reassign("pur-2026", developer.getId()));

		assertThat(result.get("projectCode").asString()).isEqualTo("PUR-2026");
		assertThat(result.get("pms").valueStream().map(pm -> pm.get("id").asLong()).toList())
			.containsExactly(developer.getId());
		final Cookie member = loginAs(developer);
		assertThat(memberRoles(member, projectId)).containsExactly(
			developer.getId() + ":PM",
			businessOwner.getId() + ":BUSINESS_OWNER"
		);
		assertThat(historiesOf(member, projectId).valueStream()
			.map(h -> h.get("changeType").asString() + ":" + h.get("userId").asLong() + ":"
				+ text(h.get("beforeActor")) + "->" + text(h.get("afterActor")) + ":" + h.get("changedBy").asLong())
			.toList())
			.containsExactly(
				"PM_REASSIGNED:" + creator.getId() + ":PM->null:" + admin.getId(),
				"PM_REASSIGNED:" + developer.getId() + ":DEVELOPER->PM:" + admin.getId()
			);
		assertThat(auditLogs(AuditAction.PROJECT_PM_REASSIGNED)).singleElement().satisfies(log -> {
			assertThat(log.getActorUserId()).isEqualTo(admin.getId());
			assertThat(log.getDetails()).contains("\"addedAsMember\":false");
		});
	}

	@Test
	@DisplayName("인사 발령으로 정해진 새 PM이 멤버가 아니면 PM 역할로 추가한다")
	void reassignToNonMember() throws Exception {
		deactivate(creator);

		reassign("PUR-2026", outsider.getId()).andExpect(status().isOk());

		final Cookie newPm = loginAs(outsider);
		assertThat(memberRoles(newPm, projectId)).containsExactly(
			developer.getId() + ":DEVELOPER",
			businessOwner.getId() + ":BUSINESS_OWNER",
			outsider.getId() + ":PM"
		);
		assertThat(historiesOf(newPm, projectId).get(1).get("beforeActor").isNull()).isTrue();
		assertThat(auditLogs(AuditAction.PROJECT_PM_REASSIGNED)).singleElement()
			.satisfies(log -> assertThat(log.getDetails()).contains("\"addedAsMember\":true"));
	}

	@Test
	@DisplayName("복수 PM 중 비활성 PM은 모두 빠지고 새 PM 한 명만 남는다")
	void reassignRemovesAllInactivePms() throws Exception {
		final Cookie pmCookie = loginAs(creator);
		patchJson(pmCookie, body("actor", "PM"), PROJECTS + "/{id}/members/{userId}/role", projectId, developer.getId())
			.andExpect(status().isOk());
		deactivate(creator);
		deactivate(developer);

		final JsonNode result = readResponse(reassign("PUR-2026", outsider.getId()));

		assertThat(result.get("pms").valueStream().map(pm -> pm.get("id").asLong()).toList())
			.containsExactly(outsider.getId());
		assertThat(memberRoles(loginAs(outsider), projectId)).containsExactly(
			businessOwner.getId() + ":BUSINESS_OWNER",
			outsider.getId() + ":PM"
		);
		assertThat(auditLogs(AuditAction.PROJECT_PM_REASSIGNED)).singleElement().satisfies(log -> assertThat(log.getDetails())
			.contains("\"removedPmUserIds\":[" + creator.getId() + "," + developer.getId() + "]"));
	}

	@Test
	@DisplayName("복수 PM 중 한 명이라도 활성이면 재지정할 수 없다(PM이 직접 추가·해제한다)")
	void notAllowedWhileAnyPmActive() throws Exception {
		patchJson(loginAs(creator), body("actor", "PM"), PROJECTS + "/{id}/members/{userId}/role", projectId, developer.getId())
			.andExpect(status().isOk());
		deactivate(creator);

		expectError(reassign("PUR-2026", outsider.getId()), ErrorCode.PM_REASSIGN_NOT_ALLOWED);
	}

	@Test
	@DisplayName("없는 코드와 PM이 활성인 프로젝트는 같은 409로 거부해 프로젝트 존재를 드러내지 않는다")
	void notAllowedWithoutRevealingProject() throws Exception {
		expectError(reassign("PUR-2026", developer.getId()), ErrorCode.PM_REASSIGN_NOT_ALLOWED);
		expectError(reassign("NONE-1", developer.getId()), ErrorCode.PM_REASSIGN_NOT_ALLOWED);
		assertThat(auditLogs(AuditAction.PROJECT_PM_REASSIGNED)).isEmpty();
	}

	@Test
	@DisplayName("새 PM 규칙: 게이트 승인자는 PM이 될 수 없고, 경영진·비활성·없는 사용자는 거부한다")
	void newPmRules() throws Exception {
		deactivate(creator);
		expectError(reassign("PUR-2026", businessOwner.getId()), ErrorCode.PM_TARGET_IS_GATE_APPROVER);
		expectError(reassign("PUR-2026", itExecutive.getId()), ErrorCode.INVALID_PROJECT_MEMBER);
		expectError(reassign("PUR-2026", 999_999L), ErrorCode.INVALID_PROJECT_MEMBER);
		deactivate(developer);
		expectError(reassign("PUR-2026", developer.getId()), ErrorCode.INVALID_PROJECT_MEMBER);
		putJson(adminCookie, body("projectCode", "!bad", "newPmUserId", developer.getId()), REASSIGN)
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("시스템 관리자가 아니면 관리자 API 존재를 숨기고 404")
	void onlySystemAdmin() throws Exception {
		expectError(putJson(loginAs(businessOwner), body("projectCode", "PUR-2026", "newPmUserId", businessOwner.getId()),
			REASSIGN), ErrorCode.NOT_FOUND);
	}

	private ResultActions reassign(final String code, final Long newPmUserId) throws Exception {
		return putJson(adminCookie, body("projectCode", code, "newPmUserId", newPmUserId), REASSIGN);
	}
}
