package org.wonik.coreviaai_be.domain.project.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * FR-PRJ-04 멤버 초대·역할 변경·제외·이력을 HTTP 경계에서 검증한다.
 * <p>
 * 준비: 전사 공개 프로젝트. 액터 PM·현업 담당자·개발자(QA 미지정), 멤버 creator(PM)·developer(개발자)·
 * businessOwner(현업 담당자, 5개 게이트 승인자). 초대 후보 newcomer(개발팀 일반 사용자).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectMemberApiIntegrationTest extends ProjectApiTestSupport {

	private static final String MEMBERS = PROJECTS + "/{id}/members";
	private static final String MEMBER = PROJECTS + "/{id}/members/{userId}";
	private static final String MEMBER_ROLE = MEMBER + "/role";

	private long projectId;
	private User newcomer;
	private Cookie pm;

	@BeforeEach
	void createProjectWithMembers() throws Exception {
		newcomer = saveUser("new01", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		projectId = createProject(validCreateBody("PUR-2026"));
		pm = loginAs(creator);
	}

	@Test
	@DisplayName("초대: PM이 여러 명을 초대하면 등록순 목록 끝에 붙고, 이력·감사 로그가 남으며 담당 업무 수는 0이다")
	void inviteMembers() throws Exception {
		final User secondNewcomer = saveUser("new02", USER_PASSWORD, SystemRole.USER, salesTeam.getId());

		final JsonNode invited = readResponse(invite(pm, member(secondNewcomer.getId(), "BUSINESS_OWNER"),
			member(newcomer.getId(), "DEVELOPER")));

		assertThat(invited.valueStream().map(m -> m.get("userId").asLong()).toList())
			.containsExactly(newcomer.getId(), secondNewcomer.getId());
		assertThat(memberRoles(pm, projectId)).containsExactly(
			creator.getId() + ":PM",
			developer.getId() + ":DEVELOPER",
			businessOwner.getId() + ":BUSINESS_OWNER",
			newcomer.getId() + ":DEVELOPER",
			secondNewcomer.getId() + ":BUSINESS_OWNER"
		);
		assertThat(membersOf(pm, projectId).valueStream().map(m -> m.get("assignedTaskCount").asLong()).toList())
			.containsOnly(0L);

		final JsonNode histories = historiesOf(pm, projectId);
		assertThat(histories).hasSize(2);
		assertThat(histories.valueStream().toList()).allSatisfy(history -> {
			assertThat(history.get("changeType").asString()).isEqualTo("INVITED");
			assertThat(history.get("beforeActor").isNull()).isTrue();
			assertThat(history.get("changedBy").asLong()).isEqualTo(creator.getId());
			assertThat(history.get("changedByName").asString()).isEqualTo(creator.getName());
		});
		assertThat(auditLogs(AuditAction.PROJECT_MEMBER_INVITED)).singleElement().satisfies(log -> {
			assertThat(log.getTargetType()).isEqualTo(AuditTargetType.PROJECT);
			assertThat(log.getTargetId()).isEqualTo(projectId);
			assertThat(log.getActorUserId()).isEqualTo(creator.getId());
		});
	}

	@Test
	@DisplayName("초대 규칙: 기존 멤버(재전송 포함)는 409, 요청 안 중복·PM 역할·미지정 액터·초대 불가 사용자는 400이고 아무도 추가되지 않는다")
	void inviteRules() throws Exception {
		invite(pm, member(newcomer.getId(), "DEVELOPER")).andExpect(status().isOk());
		expectError(invite(pm, member(newcomer.getId(), "DEVELOPER")), ErrorCode.ALREADY_PROJECT_MEMBER);

		final User other = saveUser("new02", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		expectError(invite(pm, member(other.getId(), "DEVELOPER"), member(other.getId(), "BUSINESS_OWNER")),
			ErrorCode.DUPLICATE_PROJECT_MEMBER);
		expectError(invite(pm, member(other.getId(), "PM")), ErrorCode.PM_ROLE_NOT_INVITABLE);
		expectError(invite(pm, member(other.getId(), "QA")), ErrorCode.MEMBER_ACTOR_NOT_ASSIGNED);
		expectError(invite(pm, member(itExecutive.getId(), "DEVELOPER")), ErrorCode.INVALID_PROJECT_MEMBER);
		expectError(invite(pm, member(999_999L, "DEVELOPER")), ErrorCode.INVALID_PROJECT_MEMBER);
		deactivate(other);
		expectError(invite(pm, member(other.getId(), "DEVELOPER")), ErrorCode.INVALID_PROJECT_MEMBER);
		// 한 명이라도 거부되면 같이 보낸 정상 대상도 추가하지 않는다.
		final User valid = saveUser("new03", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		expectError(invite(pm, member(valid.getId(), "DEVELOPER"), member(other.getId(), "DEVELOPER")),
			ErrorCode.INVALID_PROJECT_MEMBER);
		assertThat(memberRoles(pm, projectId)).hasSize(4);

		postJson(PROJECTS + "/" + projectId + "/members", pm, body("members", List.of())).andExpect(status().isBadRequest());
		final List<Map<String, Object>> tooMany = IntStream.rangeClosed(1, 101)
			.mapToObj(i -> member((long)i, "DEVELOPER"))
			.toList();
		postJson(PROJECTS + "/" + projectId + "/members", pm, body("members", tooMany)).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("변경 권한: 멤버·경영진은 볼 수 있어도 PM이 아니면 403, 볼 수 없는 프로젝트는 404")
	void onlyPmCanChange() throws Exception {
		final Cookie developerCookie = loginAs(developer);
		expectError(invite(developerCookie, member(newcomer.getId(), "DEVELOPER")), ErrorCode.PROJECT_PM_ONLY);
		expectError(changeRole(developerCookie, businessOwner, "DEVELOPER"), ErrorCode.PROJECT_PM_ONLY);
		expectError(deleteAs(developerCookie, MEMBER, projectId, businessOwner.getId()), ErrorCode.PROJECT_PM_ONLY);
		expectError(invite(loginAs(itExecutive), member(newcomer.getId(), "DEVELOPER")), ErrorCode.PROJECT_PM_ONLY);

		final Map<String, Object> membersOnly = validCreateBody("MEM-1");
		membersOnly.put("visibility", "MEMBERS_ONLY");
		final long hiddenId = createProject(membersOnly);
		final Cookie outsider = loginAs(noPermission);
		expectError(postJson(PROJECTS + "/" + hiddenId + "/members", outsider,
			body("members", List.of(member(newcomer.getId(), "DEVELOPER")))), ErrorCode.PROJECT_NOT_FOUND);
		expectError(getAs(outsider, MEMBERS, hiddenId), ErrorCode.PROJECT_NOT_FOUND);
		expectError(getAs(outsider, MEMBERS + "/histories", hiddenId), ErrorCode.PROJECT_NOT_FOUND);
	}

	@Test
	@DisplayName("역할 변경: 지정 액터로 바뀌고 이력(전·후)이 남는다. 게이트 승인자도 바꿀 수 있고, 같은 역할이면 이력이 없다")
	void changeRoleOfMember() throws Exception {
		changeRole(pm, developer, "BUSINESS_OWNER")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.actor").value("BUSINESS_OWNER"));
		changeRole(pm, businessOwner, "DEVELOPER").andExpect(status().isOk());
		changeRole(pm, businessOwner, "DEVELOPER").andExpect(status().isOk());

		assertThat(memberRoles(pm, projectId)).containsExactly(
			creator.getId() + ":PM",
			developer.getId() + ":BUSINESS_OWNER",
			businessOwner.getId() + ":DEVELOPER"
		);
		final JsonNode latest = historiesOf(pm, projectId).get(0);
		assertThat(latest.get("userId").asLong()).isEqualTo(businessOwner.getId());
		assertThat(latest.get("changeType").asString()).isEqualTo("ROLE_CHANGED");
		assertThat(latest.get("beforeActor").asString()).isEqualTo("BUSINESS_OWNER");
		assertThat(latest.get("afterActor").asString()).isEqualTo("DEVELOPER");
		assertThat(historiesOf(pm, projectId)).hasSize(2);
		assertThat(auditLogs(AuditAction.PROJECT_MEMBER_ROLE_CHANGED)).hasSize(2);
	}

	@Test
	@DisplayName("역할 변경 규칙: PM 역할은 위임으로만 바뀌고, 미지정 액터·멤버가 아닌 사용자는 거부한다")
	void changeRoleRules() throws Exception {
		expectError(changeRole(pm, creator, "DEVELOPER"), ErrorCode.PM_ROLE_NOT_CHANGEABLE);
		expectError(changeRole(pm, developer, "PM"), ErrorCode.PM_ROLE_NOT_CHANGEABLE);
		expectError(changeRole(pm, developer, "QA"), ErrorCode.MEMBER_ACTOR_NOT_ASSIGNED);
		expectError(changeRole(pm, newcomer, "DEVELOPER"), ErrorCode.PROJECT_MEMBER_NOT_FOUND);
		patchJson(pm, body("actor", null), MEMBER_ROLE, projectId, developer.getId()).andExpect(status().isBadRequest());
		assertThat(historiesOf(pm, projectId)).isEmpty();
	}

	@Test
	@DisplayName("제외: 멤버에서 빠지면 멤버 전용 프로젝트를 더는 못 보고, 이력이 남는다")
	void removeMember() throws Exception {
		final Map<String, Object> membersOnly = validCreateBody("MEM-1");
		membersOnly.put("visibility", "MEMBERS_ONLY");
		final long hiddenId = createProject(membersOnly);
		final Cookie developerCookie = loginAs(developer);
		getAs(developerCookie, PROJECTS + "/{id}", hiddenId).andExpect(status().isOk());

		deleteAs(pm, MEMBER, hiddenId, developer.getId()).andExpect(status().isOk());

		assertThat(memberRoles(pm, hiddenId)).doesNotContain(developer.getId() + ":DEVELOPER");
		expectError(getAs(developerCookie, PROJECTS + "/{id}", hiddenId), ErrorCode.PROJECT_NOT_FOUND);
		final JsonNode history = historiesOf(pm, hiddenId).get(0);
		assertThat(history.get("changeType").asString()).isEqualTo("REMOVED");
		assertThat(history.get("beforeActor").asString()).isEqualTo("DEVELOPER");
		assertThat(history.get("afterActor").isNull()).isTrue();
		assertThat(auditLogs(AuditAction.PROJECT_MEMBER_REMOVED)).hasSize(1);
	}

	@Test
	@DisplayName("제외 규칙: PM은 400, 게이트 승인자는 409(승인자 변경 먼저), 멤버가 아니면 404")
	void removeRules() throws Exception {
		expectError(deleteAs(pm, MEMBER, projectId, creator.getId()), ErrorCode.CANNOT_REMOVE_PM);
		expectError(deleteAs(pm, MEMBER, projectId, businessOwner.getId()), ErrorCode.GATE_APPROVER_NOT_REMOVABLE);
		expectError(deleteAs(pm, MEMBER, projectId, newcomer.getId()), ErrorCode.PROJECT_MEMBER_NOT_FOUND);
		assertThat(memberRoles(pm, projectId)).hasSize(3);
	}

	@Test
	@DisplayName("이력 커서: 최신순으로 끊어 읽고, 경영진도 조회할 수 있다")
	void historyPaging() throws Exception {
		invite(pm, member(newcomer.getId(), "DEVELOPER")).andExpect(status().isOk());
		changeRole(pm, newcomer, "BUSINESS_OWNER").andExpect(status().isOk());
		deleteAs(pm, MEMBER, projectId, newcomer.getId()).andExpect(status().isOk());

		final Cookie executive = loginAs(itExecutive);
		final JsonNode first = readResponse(getAs(executive, MEMBERS + "/histories?size=2", projectId));
		assertThat(changeTypes(first)).containsExactly("REMOVED", "ROLE_CHANGED");
		assertThat(first.get("hasNext").asBoolean()).isTrue();

		final JsonNode second = readResponse(getAs(executive,
			MEMBERS + "/histories?size=2&cursor=" + first.get("nextCursor").asString(), projectId));
		assertThat(changeTypes(second)).containsExactly("INVITED");
		assertThat(second.get("hasNext").asBoolean()).isFalse();

		expectError(getAs(executive, MEMBERS + "/histories?cursor=broken", projectId), ErrorCode.INVALID_CURSOR);
		getAs(executive, MEMBERS + "/histories?size=101", projectId).andExpect(status().isBadRequest());
	}

	@SafeVarargs
	private ResultActions invite(
		final Cookie cookie,
		final Map<String, Object>... members
	) throws Exception {
		return postJson(PROJECTS + "/" + projectId + "/members", cookie, body("members", List.of(members)));
	}

	private ResultActions changeRole(
		final Cookie cookie,
		final User target,
		final String actor
	) throws Exception {
		return patchJson(cookie, body("actor", actor), MEMBER_ROLE, projectId, target.getId());
	}

	private static List<String> changeTypes(final JsonNode page) {
		return page.get("items").valueStream().map(item -> item.get("changeType").asString()).toList();
	}
}
