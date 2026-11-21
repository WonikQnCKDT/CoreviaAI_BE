package org.wonik.coreviaai_be.domain.project.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
 * FR-PRJ-04 복수 PM(요구사항명세 최종안 3.2 공통 규칙)을 HTTP·DB 경계에서 검증한다(통합 테스트).
 * <ul>
 *   <li>PM은 다른 멤버의 역할을 PM으로 바꿔 추가하고, PM을 다른 지정 액터로 바꿔 해제한다</li>
 *   <li>프로젝트당 PM은 최소 1명: 마지막 PM은 해제·제외할 수 없다</li>
 *   <li>복수 PM은 동일 권한, 게이트 승인자는 PM이 될 수 없다</li>
 * </ul>
 * 준비는 ProjectMemberApiIntegrationTest와 같다(액터 PM·현업 담당자·개발자, 게이트 승인자 = businessOwner).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectMultiPmApiIntegrationTest extends ProjectApiTestSupport {

	private static final String MEMBER = PROJECTS + "/{id}/members/{userId}";
	private static final String MEMBER_ROLE = MEMBER + "/role";

	private long projectId;
	private Cookie pm;

	@BeforeEach
	void createProjectWithMembers() throws Exception {
		projectId = createProject(validCreateBody("PUR-2026"));
		pm = loginAs(creator);
	}

	@Test
	@DisplayName("PM 추가: 멤버를 PM으로 바꾸면 상세의 PM이 2명이 되고, PM_ADDED 이력과 감사 로그가 남는다")
	void addPm() throws Exception {
		changeRole(pm, developer, "PM").andExpect(status().isOk());

		assertThat(pmIdsOf(pm, projectId)).containsExactly(creator.getId(), developer.getId());
		final JsonNode history = historiesOf(pm, projectId).get(0);
		assertThat(history.get("changeType").asString()).isEqualTo("PM_ADDED");
		assertThat(history.get("beforeActor").asString()).isEqualTo("DEVELOPER");
		assertThat(history.get("afterActor").asString()).isEqualTo("PM");
		assertThat(auditLogs(AuditAction.PROJECT_PM_ADDED)).singleElement()
			.satisfies(log -> assertThat(log.getActorUserId()).isEqualTo(creator.getId()));
		assertThat(auditLogs(AuditAction.PROJECT_MEMBER_ROLE_CHANGED)).isEmpty();
	}

	@Test
	@DisplayName("복수 PM은 동일 권한: 추가된 PM도 초대·역할 변경을 할 수 있다")
	void addedPmHasSameAuthority() throws Exception {
		changeRole(pm, developer, "PM").andExpect(status().isOk());
		final User newcomer = saveUser("new01", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		final Cookie secondPm = loginAs(developer);

		postJson(PROJECTS + "/" + projectId + "/members", secondPm,
			body("members", List.of(member(newcomer.getId(), "DEVELOPER")))).andExpect(status().isOk());
		changeRole(secondPm, newcomer, "BUSINESS_OWNER").andExpect(status().isOk());
	}

	@Test
	@DisplayName("PM 해제: PM이 2명 이상이면 본인도 해제할 수 있고, 해제 후에는 관리 권한이 없다")
	void releasePm() throws Exception {
		changeRole(pm, developer, "PM").andExpect(status().isOk());

		changeRole(pm, creator, "DEVELOPER").andExpect(status().isOk());

		assertThat(pmIdsOf(pm, projectId)).containsExactly(developer.getId());
		assertThat(historiesOf(pm, projectId).get(0).get("changeType").asString()).isEqualTo("PM_RELEASED");
		assertThat(auditLogs(AuditAction.PROJECT_PM_RELEASED)).hasSize(1);
		expectError(changeRole(pm, businessOwner, "DEVELOPER"), ErrorCode.PROJECT_PM_ONLY);
		// PM 필터 목록도 바뀐 PM 기준으로 조회된다.
		assertThat(readResponse(getAs(pm, PROJECTS + "?pmUserId=" + developer.getId())).get("items")).hasSize(1);
		assertThat(readResponse(getAs(pm, PROJECTS + "?pmUserId=" + creator.getId())).get("items")).isEmpty();
	}

	@Test
	@DisplayName("최소 1명: 마지막 PM은 해제·제외할 수 없고(409), 아무것도 바뀌지 않는다")
	void lastPmIsProtected() throws Exception {
		expectError(changeRole(pm, creator, "DEVELOPER"), ErrorCode.LAST_PM_REQUIRED);
		expectError(deleteAs(pm, MEMBER, projectId, creator.getId()), ErrorCode.LAST_PM_REQUIRED);

		assertThat(pmIdsOf(pm, projectId)).containsExactly(creator.getId());
		assertThat(historiesOf(pm, projectId)).isEmpty();
	}

	@Test
	@DisplayName("PM이 2명이면 다른 PM을 멤버에서 제외할 수 있다")
	void removeOneOfTwoPms() throws Exception {
		changeRole(pm, developer, "PM").andExpect(status().isOk());

		deleteAs(pm, MEMBER, projectId, developer.getId()).andExpect(status().isOk());

		assertThat(pmIdsOf(pm, projectId)).containsExactly(creator.getId());
		final JsonNode history = historiesOf(pm, projectId).get(0);
		assertThat(history.get("changeType").asString()).isEqualTo("REMOVED");
		assertThat(history.get("beforeActor").asString()).isEqualTo("PM");
	}

	@Test
	@DisplayName("PM 추가 규칙: 게이트 승인자·비활성 멤버는 PM이 될 수 없다")
	void addPmRules() throws Exception {
		expectError(changeRole(pm, businessOwner, "PM"), ErrorCode.PM_TARGET_IS_GATE_APPROVER);
		deactivate(developer);
		expectError(changeRole(pm, developer, "PM"), ErrorCode.INVALID_PM_TARGET);

		assertThat(pmIdsOf(pm, projectId)).containsExactly(creator.getId());
		assertThat(auditLogs(AuditAction.PROJECT_PM_ADDED)).isEmpty();
	}

	@Test
	@DisplayName("PM 위임 API는 폐지됐다(복수 PM 추가·해제로 대체)")
	void delegationApiRemoved() throws Exception {
		final Map<String, Object> legacy = body("newPmUserId", developer.getId(), "previousPmAction", "LEAVE");
		postJson(PROJECTS + "/" + projectId + "/pm-delegation", pm, legacy).andExpect(status().is4xxClientError());
		assertThat(pmIdsOf(pm, projectId)).containsExactly(creator.getId());
	}

	@Test
	@DisplayName("동시성: 두 PM이 동시에 서로를 해제해도 PM이 0명이 되지 않는다(프로젝트 행 잠금)")
	void concurrentReleaseKeepsOnePm() throws Exception {
		changeRole(pm, developer, "PM").andExpect(status().isOk());
		final Cookie secondPm = loginAs(developer);

		final ExecutorService executor = Executors.newFixedThreadPool(2);
		final CountDownLatch start = new CountDownLatch(1);
		try {
			final Future<Integer> first = executor.submit(releaseAfter(start, pm, developer));
			final Future<Integer> second = executor.submit(releaseAfter(start, secondPm, creator));
			start.countDown();
			final List<Integer> statuses = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));

			// 한 요청만 성공하고, 나머지는 PM이 아니게 됐거나(403) 마지막 PM이라(409) 거부된다.
			assertThat(statuses).containsOnlyOnce(200);
		} finally {
			executor.shutdownNow();
		}
		assertThat(pmIdsOf(loginAs(businessOwner), projectId)).hasSize(1);
	}

	private Callable<Integer> releaseAfter(final CountDownLatch start, final Cookie cookie, final User target) {
		return () -> {
			start.await();
			return changeRole(cookie, target, "DEVELOPER").andReturn().getResponse().getStatus();
		};
	}

	private ResultActions changeRole(final Cookie cookie, final User target, final String actor) throws Exception {
		return patchJson(cookie, body("actor", actor), MEMBER_ROLE, projectId, target.getId());
	}
}
