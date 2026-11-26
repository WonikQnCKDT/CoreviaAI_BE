package org.wonik.coreviaai_be.domain.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.notification.service.DueSoonNotificationPublisher;
import org.wonik.coreviaai_be.domain.project.controller.ProjectApiTestSupport;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * FR-NTF-01 인앱 알림. 배정·상태 변경·마감 D-1, 미확인 개수, 본인 목록, 다른 사람 알림은 404.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationApiIntegrationTest extends ProjectApiTestSupport {

	private static final String NOTIFICATIONS = "/api/notifications";

	@Autowired
	private DueSoonNotificationPublisher dueSoonPublisher;

	private long projectId;
	private Cookie pm;
	private Cookie developerCookie;

	@BeforeEach
	void createProject() throws Exception {
		final Map<String, Object> body = validCreateBody("NTF-2026");
		body.put("members", List.of(
			member(developer.getId(), "DEVELOPER"),
			member(businessOwner.getId(), "BUSINESS_OWNER")
		));
		projectId = createProject(body);
		pm = loginAs(creator);
		developerCookie = loginAs(developer);
	}

	@Test
	@DisplayName("배정과 상태 변경은 담당자에게만 알리고, 본인 작업과 다른 사람 알림은 보여 주지 않는다")
	void assignmentAndStatusChangeReachAssigneeOnly() throws Exception {
		final long taskId = readResponse(postJson("/api/projects/" + projectId + "/wbs/tasks", pm, body(
			"name", "설계", "description", null, "assigneeUserId", developer.getId(),
			"startDate", "2026-12-01", "endDate", "2026-12-10", "estimatedEffort", null
		))).get("taskId").asLong();
		readResponse(postJson("/api/projects/" + projectId + "/wbs/tasks", pm, body(
			"name", "PM 본인 업무", "description", null, "assigneeUserId", creator.getId(),
			"startDate", "2026-12-01", "endDate", "2026-12-10", "estimatedEffort", null
		)));

		final long inProgressColumnId = jdbcTemplate.queryForObject(
			"select id from board_columns where project_id = ? and status_category = 'IN_PROGRESS' order by id limit 1",
			Long.class,
			projectId
		);
		final long version = jdbcTemplate.queryForObject(
			"select version from wbs_tasks where id = ?",
			Long.class,
			taskId
		);
		putJson(pm, body("columnId", inProgressColumnId, "version", version),
			"/api/projects/{projectId}/board/cards/{taskId}/column", projectId, taskId)
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

		final JsonNode count = readResponse(getAs(developerCookie, NOTIFICATIONS + "/unread-count"));
		assertThat(count.get("unreadCount").asLong()).isEqualTo(2);
		assertThat(readResponse(getAs(pm, NOTIFICATIONS + "/unread-count")).get("unreadCount").asLong()).isZero();

		final JsonNode page = readResponse(getAs(developerCookie, NOTIFICATIONS + "?size=10"));
		assertThat(page.get("items")).hasSize(2);
		assertThat(page.get("items").get(0).get("notificationType").asString()).isEqualTo("STATUS_CHANGED");
		assertThat(page.get("items").get(0).get("messageParams").get("status").asString()).isEqualTo("IN_PROGRESS");
		assertThat(page.get("items").get(1).get("notificationType").asString()).isEqualTo("ASSIGNED");
		assertThat(page.get("items").get(1).get("targetType").asString()).isEqualTo("TASK");
		assertThat(page.get("items").get(1).get("targetId").asLong()).isEqualTo(taskId);
		assertThat(page.get("hasNext").asBoolean()).isFalse();

		final long notificationId = page.get("items").get(1).get("id").asLong();
		final JsonNode read = readResponse(postJson(NOTIFICATIONS + "/" + notificationId + "/read", developerCookie, Map.of()));
		assertThat(read.get("readAt").isNull()).isFalse();
		final JsonNode readAgain = readResponse(
			postJson(NOTIFICATIONS + "/" + notificationId + "/read", developerCookie, Map.of())
		);
		assertThat(readAgain.get("readAt").asString()).isEqualTo(read.get("readAt").asString());
		assertThat(readResponse(getAs(developerCookie, NOTIFICATIONS + "/unread-count")).get("unreadCount").asLong())
			.isEqualTo(1);
		expectError(
			postJson(NOTIFICATIONS + "/" + notificationId + "/read", pm, Map.of()),
			ErrorCode.NOTIFICATION_NOT_FOUND
		);
		expectError(getAs(null, NOTIFICATIONS), ErrorCode.UNAUTHORIZED);

		saveUser("admin01", "Adm1n-pass!", SystemRole.SYSTEM_ADMIN, null);
		final Cookie admin = loginOk("admin01", "Adm1n-pass!");
		assertThat(readResponse(getAs(admin, NOTIFICATIONS + "/unread-count")).get("unreadCount").asLong()).isZero();
	}

	@Test
	@DisplayName("마감 하루 전 미완료 담당 업무만 알리고 같은 날 다시 실행해도 늘지 않는다")
	void dueSoonNotifiesOncePerTaskPerDay() throws Exception {
		readResponse(postJson("/api/projects/" + projectId + "/wbs/tasks", pm, body(
			"name", "마감 업무", "description", null, "assigneeUserId", developer.getId(),
			"startDate", "2026-12-01", "endDate", "2026-12-10", "estimatedEffort", null
		)));
		clock.setInstant(Instant.parse("2026-12-09T00:00:00Z"));

		dueSoonPublisher.publishDueTomorrow();
		dueSoonPublisher.publishDueTomorrow();

		final JsonNode items = readResponse(getAs(loginAs(developer), NOTIFICATIONS + "?size=10")).get("items");
		assertThat(items).hasSize(2);
		assertThat(items.get(0).get("notificationType").asString()).isEqualTo("DUE_SOON");
		assertThat(items.get(0).get("messageKey").asString()).isEqualTo("notification.dueSoon");
		assertThat(items.get(0).get("messageParams").get("endDate").asString()).isEqualTo("2026-12-10");
		assertThat(items.get(1).get("notificationType").asString()).isEqualTo("ASSIGNED");
	}
}
