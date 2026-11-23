package org.wonik.coreviaai_be.domain.project.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.common.exception.ErrorCode;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectPhaseApiIntegrationTest extends ProjectApiTestSupport {

	private long projectId;
	private Cookie pm;

	@BeforeEach
	void createProject() throws Exception {
		projectId = createProject(validCreateBody("PHA-1"));
		pm = loginAs(creator);
	}

	@Test
	@DisplayName("PM은 단계를 추가·이름 변경·재정렬·삭제하고 모든 변경을 감사 로그로 남긴다")
	void managePhases() throws Exception {
		final JsonNode created = readResponse(postJson(PROJECTS + "/" + projectId + "/phases", pm, body("name", " 안정화 ")));
		final long createdId = created.get("id").asLong();
		assertThat(created.get("name").asString()).isEqualTo("안정화");

		patchJson(pm, body("name", "운영 전환"), PROJECTS + "/{id}/phases/{phaseId}", projectId, createdId)
			.andExpect(status().isOk());
		final List<Long> reversed = new ArrayList<>(phasesOf(pm, projectId).valueStream()
			.map(phase -> phase.get("id").asLong()).toList());
		Collections.reverse(reversed);
		putJson(pm, body("phaseIds", reversed), PROJECTS + "/{id}/phases/order", projectId)
			.andExpect(status().isOk());
		assertThat(phasesOf(pm, projectId).valueStream().map(phase -> phase.get("id").asLong()).toList())
			.containsExactlyElementsOf(reversed);

		deleteAs(pm, PROJECTS + "/{id}/phases/{phaseId}", projectId, createdId).andExpect(status().isOk());
		assertThat(phasesOf(pm, projectId).valueStream().map(phase -> phase.get("phaseOrder").asInt()).toList())
			.containsExactly(1, 2, 3, 4, 5);
		assertThat(auditLogs(AuditAction.PROJECT_PHASE_CREATED)).hasSize(1);
		assertThat(auditLogs(AuditAction.PROJECT_PHASE_RENAMED)).hasSize(1);
		assertThat(auditLogs(AuditAction.PROJECT_PHASE_REORDERED)).hasSize(1);
		assertThat(auditLogs(AuditAction.PROJECT_PHASE_DELETED)).hasSize(1);
	}

	@Test
	@DisplayName("단계 변경은 PM만 가능하고 다른 프로젝트 단계 ID는 존재를 노출하지 않는다")
	void authorizationAndProjectBoundary() throws Exception {
		final long phaseId = phasesOf(pm, projectId).get(0).get("id").asLong();
		expectError(postJson(PROJECTS + "/" + projectId + "/phases", loginAs(developer), body("name", "추가")),
			ErrorCode.PROJECT_PM_ONLY);
		expectError(patchJson(loginAs(developer), body("name", "변경"), PROJECTS + "/{id}/phases/{phaseId}",
			projectId, phaseId), ErrorCode.PROJECT_PM_ONLY);

		final long otherProjectId = createProject(validCreateBody("PHA-2"));
		expectError(patchJson(pm, body("name", "변경"), PROJECTS + "/{id}/phases/{phaseId}",
			otherProjectId, phaseId), ErrorCode.PROJECT_PHASE_NOT_FOUND);
	}

	@Test
	@DisplayName("단계 입력은 공백·100자 초과·21개를 거부하고 재정렬은 현재 단계 전체 ID만 허용한다")
	void validationAndLimits() throws Exception {
		postJson(PROJECTS + "/" + projectId + "/phases", pm, body("name", "   ")).andExpect(status().isBadRequest());
		postJson(PROJECTS + "/" + projectId + "/phases", pm, body("name", "가".repeat(101)))
			.andExpect(status().isBadRequest());

		final List<Long> phaseIds = phasesOf(pm, projectId).valueStream().map(phase -> phase.get("id").asLong()).toList();
		expectError(putJson(pm, body("phaseIds", phaseIds.subList(0, 4)), PROJECTS + "/{id}/phases/order", projectId),
			ErrorCode.INVALID_PROJECT_PHASE_ORDER);
		expectError(putJson(pm, body("phaseIds", List.of(phaseIds.get(0), phaseIds.get(0))),
			PROJECTS + "/{id}/phases/order", projectId), ErrorCode.INVALID_PROJECT_PHASE_ORDER);

		final Map<String, Object> twenty = validCreateBody("PHA-20");
		twenty.put("phases", java.util.stream.IntStream.rangeClosed(1, 20).mapToObj(i -> "단계 " + i).toList());
		final long fullProjectId = createProject(twenty);
		expectError(postJson(PROJECTS + "/" + fullProjectId + "/phases", pm, body("name", "21번째")),
			ErrorCode.PROJECT_PHASE_LIMIT_EXCEEDED);
	}
}
