package org.wonik.coreviaai_be.domain.project.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskDetails;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectSettingsApiIntegrationTest extends ProjectApiTestSupport {

	@Autowired
	private WbsTaskRepository taskRepository;

	@Test
	@DisplayName("PM만 기본 정보·기능·게이트 승인자를 바꾸고, 기능 데이터와 변경 이력은 남긴다")
	void pmChangesSettingsAndKeepsModuleData() throws Exception {
		final Cookie pm = loginAs(creator);
		final long projectId = readResponse(create(pm, validCreateBody("SET-1"))).get("id").asLong();
		final long version = readResponse(getAs(pm, PROJECTS + "/{id}", projectId)).get("version").asLong();
		final int columnsBefore = jdbcTemplate.queryForObject(
			"select count(*) from board_columns where project_id = ?", Integer.class, projectId);

		expectError(patchJson(loginAs(developer), settings(version, "features", List.of("WBS")),
			PROJECTS + "/{id}/settings", projectId), ErrorCode.PROJECT_PM_ONLY);
		expectError(patchJson(loginOk(
			saveUser("setadmin", "Adm1n-pass!", SystemRole.SYSTEM_ADMIN, null).getLoginId(), "Adm1n-pass!"),
			settings(version, "features", List.of("WBS")), PROJECTS + "/{id}/settings", projectId), ErrorCode.NOT_FOUND);

		final var updated = readResponse(patchJson(pm, body(
			"version", version,
			"basicInfo", body(
				"startDate", "2026-12-15",
				"endDate", null,
				"departmentIds", List.of(devTeam.getId(), salesTeam.getId()),
				"visibility", "DEPARTMENT"
			),
			"features", List.of("WBS", "MEETING"),
			"actors", List.of("PM", "BUSINESS_OWNER", "DEVELOPER", "QA"),
			"gateApprover", body("userId", developer.getId())
		), PROJECTS + "/{id}/settings", projectId));
		assertThat(updated.get("endDate").isNull()).isTrue();
		assertThat(updated.get("visibility").asString()).isEqualTo("DEPARTMENT");
		assertThat(updated.get("features")).hasSize(2);
		assertThat(updated.get("version").asLong()).isGreaterThan(version);

		assertThat(jdbcTemplate.queryForObject(
			"select count(*) from board_columns where project_id = ?", Integer.class, projectId)).isEqualTo(columnsBefore);
		assertThat(readResponse(getAs(pm, PROJECTS + "/{id}/phases", projectId)).get(0).get("gateApproverUserId").asLong())
			.isEqualTo(developer.getId());

		getAs(pm, PROJECTS + "/{id}/setting-histories", projectId)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.items.length()").value(4))
			.andExpect(jsonPath("$.response.items[0].changedByName").value(creator.getName()))
			.andExpect(jsonPath("$.response.items[0].before").exists())
			.andExpect(jsonPath("$.response.items[0].after").exists());
		assertThat(auditLogs(AuditAction.PROJECT_SETTINGS_CHANGED)).hasSize(1);

		expectError(patchJson(pm, body(
			"version", updated.get("version").asLong(),
			"actors", List.of("PM", "DEVELOPER")
		), PROJECTS + "/{id}/settings", projectId), ErrorCode.ACTOR_HAS_MEMBERS);
		expectError(patchJson(pm, body("version", version, "methodology", "NONE"),
			PROJECTS + "/{id}/settings", projectId), ErrorCode.CONCURRENT_MODIFICATION);
	}

	@Test
	@DisplayName("업무가 없으면 운영 방식을 바꿀 수 있고, 업무가 있으면 거절한다")
	void methodologyChangeRequiresZeroTasks() throws Exception {
		final Cookie pm = loginAs(creator);
		final long projectId = readResponse(create(pm, validCreateBody("SET-2"))).get("id").asLong();
		final long version = readResponse(getAs(pm, PROJECTS + "/{id}", projectId)).get("version").asLong();

		final var changed = readResponse(patchJson(pm, body("version", version, "methodology", "NONE"),
			PROJECTS + "/{id}/settings", projectId));
		assertThat(changed.get("methodology").asString()).isEqualTo("NONE");

		taskRepository.save(WbsTask.task(projectId, 1, new TaskDetails(
			"분석", null, null, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 2), null),
			WbsItemSource.MANUAL, creator.getId()));
		expectError(patchJson(pm, body("version", changed.get("version").asLong(), "methodology", "WATERFALL"),
			PROJECTS + "/{id}/settings", projectId), ErrorCode.METHODOLOGY_CHANGE_NOT_ALLOWED);
	}

	@Test
	@DisplayName("바꿀 설정이 없으면 거절하고, 단계가 없으면 게이트 승인자를 지정할 수 없다")
	void rejectsEmptyChangeAndApproverWithoutPhases() throws Exception {
		final Cookie pm = loginAs(creator);
		final Map<String, Object> empty = validCreateBody("SET-3");
		empty.put("template", "EMPTY");
		empty.put("methodology", "NONE");
		empty.put("features", List.of("MEETING", "FILE"));
		empty.put("actors", List.of("PM"));
		empty.put("members", List.of());
		empty.remove("gateApproverUserId");
		final long projectId = readResponse(create(pm, empty)).get("id").asLong();
		final long version = readResponse(getAs(pm, PROJECTS + "/{id}", projectId)).get("version").asLong();

		expectError(patchJson(pm, body("version", version), PROJECTS + "/{id}/settings", projectId),
			ErrorCode.INVALID_PROJECT_SETTINGS);
		expectError(patchJson(pm, body("version", version, "gateApprover", body("userId", developer.getId())),
			PROJECTS + "/{id}/settings", projectId), ErrorCode.GATE_APPROVER_NOT_AVAILABLE);
	}

	private static Map<String, Object> settings(final long version, final String field, final Object value) {
		return body("version", version, field, value);
	}
}
