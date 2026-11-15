package org.wonik.coreviaai_be.domain.project.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

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
 * FR-PRJ-04 PM 위임(현 PM만, 기존 PM은 역할 변경 또는 멤버 제외)을 HTTP 경계에서 검증한다.
 * 준비는 ProjectMemberApiIntegrationTest와 같다(게이트 승인자 = businessOwner).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectPmDelegationApiIntegrationTest extends ProjectApiTestSupport {

	private long projectId;
	private Cookie pm;

	@BeforeEach
	void createProjectWithMembers() throws Exception {
		projectId = createProject(validCreateBody("PUR-2026"));
		pm = loginAs(creator);
	}

	@Test
	@DisplayName("역할 변경으로 위임: 새 PM이 PM이 되고 기존 PM은 지정 역할로 남으며, 두 사람의 이력과 감사 로그가 남는다")
	void delegateAndChangeRole() throws Exception {
		final JsonNode result = readResponse(delegate(pm, developer.getId(), "CHANGE_ROLE", "DEVELOPER"));

		assertThat(result.get("pmUserId").asLong()).isEqualTo(developer.getId());
		assertThat(result.get("pmName").asString()).isEqualTo(developer.getName());
		assertThat(result.get("projectCode").asString()).isEqualTo("PUR-2026");
		assertThat(readResponse(getAs(pm, PROJECTS + "/{id}", projectId)).get("pmUserId").asLong()).isEqualTo(developer.getId());
		assertThat(memberRoles(pm, projectId)).containsExactly(
			creator.getId() + ":DEVELOPER",
			developer.getId() + ":PM",
			businessOwner.getId() + ":BUSINESS_OWNER"
		);

		final JsonNode histories = historiesOf(pm, projectId);
		assertThat(histories.valueStream()
			.map(h -> h.get("changeType").asString() + ":" + h.get("userId").asLong() + ":"
				+ text(h.get("beforeActor")) + "->" + text(h.get("afterActor")))
			.toList())
			.containsExactly(
				"PM_DELEGATED:" + creator.getId() + ":PM->DEVELOPER",
				"PM_DELEGATED:" + developer.getId() + ":DEVELOPER->PM"
			);
		assertThat(auditLogs(AuditAction.PROJECT_PM_DELEGATED)).singleElement()
			.satisfies(log -> assertThat(log.getActorUserId()).isEqualTo(creator.getId()));

		// 권한이 함께 넘어간다: 기존 PM은 더 이상 관리할 수 없고 새 PM은 할 수 있다.
		final User newcomer = saveUser("new01", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		final Map<String, Object> invite = body("members", List.of(member(newcomer.getId(), "DEVELOPER")));
		expectError(postJson(PROJECTS + "/" + projectId + "/members", pm, invite), ErrorCode.PROJECT_PM_ONLY);
		postJson(PROJECTS + "/" + projectId + "/members", loginAs(developer), invite).andExpect(status().isOk());
	}

	@Test
	@DisplayName("멤버 제외로 위임: 기존 PM은 멤버에서 빠지고 PM 필터 목록이 새 PM 기준으로 바뀐다")
	void delegateAndLeave() throws Exception {
		delegate(pm, developer.getId(), "LEAVE", null).andExpect(status().isOk());

		assertThat(memberRoles(pm, projectId)).containsExactly(
			developer.getId() + ":PM",
			businessOwner.getId() + ":BUSINESS_OWNER"
		);
		assertThat(historiesOf(pm, projectId).get(0).get("afterActor").isNull()).isTrue();
		assertThat(readResponse(getAs(pm, PROJECTS + "?pmUserId=" + developer.getId())).get("items")).hasSize(1);
	}

	@Test
	@DisplayName("위임 규칙: 게이트 승인자·본인·비멤버·비활성 멤버에게는 넘길 수 없고, 거부되면 아무것도 바뀌지 않는다")
	void delegationTargetRules() throws Exception {
		expectError(delegate(pm, businessOwner.getId(), "LEAVE", null), ErrorCode.PM_TARGET_IS_GATE_APPROVER);
		expectError(delegate(pm, creator.getId(), "LEAVE", null), ErrorCode.INVALID_PM_DELEGATION_TARGET);
		expectError(delegate(pm, noPermission.getId(), "LEAVE", null), ErrorCode.INVALID_PM_DELEGATION_TARGET);
		deactivate(developer);
		expectError(delegate(pm, developer.getId(), "LEAVE", null), ErrorCode.INVALID_PM_DELEGATION_TARGET);

		assertThat(readResponse(getAs(pm, PROJECTS + "/{id}", projectId)).get("pmUserId").asLong()).isEqualTo(creator.getId());
		assertThat(historiesOf(pm, projectId)).isEmpty();
		assertThat(auditLogs(AuditAction.PROJECT_PM_DELEGATED)).isEmpty();
	}

	@Test
	@DisplayName("기존 PM 처리 규칙: 역할 변경은 PM이 아닌 지정 액터가 필요하고, 멤버 제외는 역할을 받지 않는다")
	void previousPmActionRules() throws Exception {
		expectError(delegate(pm, developer.getId(), "CHANGE_ROLE", null), ErrorCode.INVALID_PREVIOUS_PM_ACTION);
		expectError(delegate(pm, developer.getId(), "CHANGE_ROLE", "PM"), ErrorCode.INVALID_PREVIOUS_PM_ACTION);
		expectError(delegate(pm, developer.getId(), "CHANGE_ROLE", "QA"), ErrorCode.MEMBER_ACTOR_NOT_ASSIGNED);
		expectError(delegate(pm, developer.getId(), "LEAVE", "DEVELOPER"), ErrorCode.INVALID_PREVIOUS_PM_ACTION);
		postJson(PROJECTS + "/" + projectId + "/pm-delegation", pm, body("newPmUserId", developer.getId()))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("위임 권한: 현 PM이 아니면 403(멤버·경영진), 볼 수 없는 프로젝트는 404")
	void onlyCurrentPmCanDelegate() throws Exception {
		expectError(delegate(loginAs(developer), businessOwner.getId(), "LEAVE", null), ErrorCode.PROJECT_PM_ONLY);
		expectError(delegate(loginAs(itExecutive), developer.getId(), "LEAVE", null), ErrorCode.PROJECT_PM_ONLY);
		expectError(postJson(PROJECTS + "/999999/pm-delegation", pm,
			body("newPmUserId", developer.getId(), "previousPmAction", "LEAVE")), ErrorCode.PROJECT_NOT_FOUND);
	}

	private ResultActions delegate(
		final Cookie cookie,
		final Long newPmUserId,
		final String previousPmAction,
		final String previousPmActor
	) throws Exception {
		return postJson(PROJECTS + "/" + projectId + "/pm-delegation", cookie, body(
			"newPmUserId", newPmUserId,
			"previousPmAction", previousPmAction,
			"previousPmActor", previousPmActor
		));
	}
}
