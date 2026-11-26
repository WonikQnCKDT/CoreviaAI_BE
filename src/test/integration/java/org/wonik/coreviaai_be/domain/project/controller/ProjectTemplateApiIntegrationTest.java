package org.wonik.coreviaai_be.domain.project.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectTemplateApiIntegrationTest extends ProjectApiTestSupport {

	private static final String TEMPLATES = "/api/admin/project-templates";

	@Test
	@DisplayName("템플릿 수정은 이후 생성에만 적용되고, 빈 단계에는 필수 산출물을 둘 수 없다")
	void templateEditAppliesOnlyToFutureProjects() throws Exception {
		final Cookie admin = loginOk(
			saveUser("tpladmin", "Adm1n-pass!", SystemRole.SYSTEM_ADMIN, null).getLoginId(),
			"Adm1n-pass!"
		);
		final Cookie user = loginAs(creator);
		expectError(getAs(user, TEMPLATES), ErrorCode.NOT_FOUND);

		final long beforeId = readResponse(create(user, validCreateBody("TPL-BEFORE"))).get("id").asLong();
		assertThat(readResponse(getAs(user, PROJECTS + "/" + beforeId + "/phases"))).hasSize(5);

		final Map<String, Object> original = originalItTemplate();
		try {
			putJson(admin, changedItTemplate(), TEMPLATES + "/IT_PROJECT")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.response.phases.length()").value(1))
				.andExpect(jsonPath("$.response.requiredDeliverables[0].name").value("요구사항 정의서"));
			assertThat(auditLogs(AuditAction.PROJECT_TEMPLATE_CHANGED)).hasSize(1);

			assertThat(readResponse(getAs(user, PROJECTS + "/" + beforeId + "/phases"))).hasSize(5);
			getAs(user, PROJECTS + "/creation-options")
				.andExpect(jsonPath("$.response.templates[1].defaultPhases.length()").value(1));

			final long afterId = readResponse(create(user, validCreateBody("TPL-AFTER"))).get("id").asLong();
			final var afterPhases = readResponse(getAs(user, PROJECTS + "/" + afterId + "/phases"));
			assertThat(afterPhases).hasSize(1);
			assertThat(afterPhases.get(0).get("name").asString()).isEqualTo("기획");

			expectError(putJson(admin, emptyTemplateWithDeliverable(), TEMPLATES + "/EMPTY"), ErrorCode.INVALID_TEMPLATE_CONFIG);
			expectError(putJson(admin, changedItTemplate(), TEMPLATES + "/IT_PROJECT"), ErrorCode.CONCURRENT_MODIFICATION);
		} finally {
			putJson(admin, original, TEMPLATES + "/IT_PROJECT").andExpect(status().isOk());
		}
	}

	private Map<String, Object> changedItTemplate() {
		return body(
			"version", 0,
			"name", "IT 프로젝트 템플릿",
			"methodology", "WATERFALL",
			"departmentType", "SW",
			"phases", List.of("기획"),
			"actors", List.of("PM", "DEVELOPER"),
			"features", List.of("WBS", "KANBAN"),
			"requiredDeliverables", List.of(body("phaseName", "기획", "name", "요구사항 정의서"))
		);
	}

	private Map<String, Object> originalItTemplate() {
		return body(
			"version", 1,
			"name", "IT 프로젝트 템플릿",
			"methodology", "WATERFALL",
			"departmentType", "SW",
			"phases", List.of("분석", "설계", "개발", "테스트", "이행"),
			"actors", List.of("PM", "BUSINESS_OWNER", "DEVELOPER", "QA"),
			"features", List.of("WBS", "KANBAN", "BOARD", "MEETING", "DELIVERABLE", "ISSUE", "FILE"),
			"requiredDeliverables", List.of()
		);
	}

	private Map<String, Object> emptyTemplateWithDeliverable() {
		return body(
			"version", 0,
			"name", "빈 템플릿",
			"methodology", "NONE",
			"phases", List.of(),
			"actors", List.of("PM"),
			"features", List.of("MEETING", "FILE"),
			"requiredDeliverables", List.of(body("phaseName", "분석", "name", "요구사항 정의서"))
		);
	}
}
