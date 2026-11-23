package org.wonik.coreviaai_be.domain.wbs.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.project.controller.ProjectApiTestSupport;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsDependencyRepository;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.time.BusinessCalendar;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/** FR-WBS-03 완료→시작 선후행 관계를 HTTP·권한·DB 경계에서 검증한다. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskDependencyApiIntegrationTest extends ProjectApiTestSupport {

	private static final String TASKS = PROJECTS + "/{projectId}/wbs/tasks";
	private static final String PREDECESSORS = TASKS + "/{taskId}/predecessors";

	@Autowired
	private WbsDependencyRepository dependencyRepository;

	private long projectId;
	private Cookie pm;

	@BeforeEach
	void createWbsProject() throws Exception {
		projectId = createProject(validCreateBody("DEP-2026"));
		pm = loginAs(creator);
	}

	@Test
	@DisplayName("전체 교체: 중복은 한 번만 저장하고 남는 관계는 유지하며 빈 배열로 모두 해제한다")
	void replaceAndClear() throws Exception {
		final long a = createTask("A", creator.getId(), "2026-12-01", "2026-12-10");
		final long b = createTask("B", developer.getId(), "2026-12-11", "2026-12-20");
		final long c = createTask("C", developer.getId(), "2026-12-21", "2026-12-31");

		JsonNode response = replace(pm, c, List.of(b, a, b));
		assertThat(ids(response)).containsExactly(a, b);
		assertThat(dependencyRepository.count()).isEqualTo(2);

		// 같은 요청 재전송은 행을 다시 만들지 않는다.
		response = replace(pm, c, List.of(a, b));
		assertThat(ids(response)).containsExactly(a, b);
		assertThat(dependencyRepository.count()).isEqualTo(2);

		// A는 유지하고 B 대신 새 관계를 넣은 뒤 모두 해제한다.
		response = replace(pm, c, List.of(a));
		assertThat(ids(response)).containsExactly(a);
		assertThat(dependencyRepository.count()).isEqualTo(1);
		assertThat(replace(pm, c, List.of()).get("predecessors")).isEmpty();
		assertThat(dependencyRepository.count()).isZero();
	}

	@Test
	@DisplayName("지연 영향: 선행 작업 종료일이 지났고 미완료이면 지연 일수와 후행 영향 표시, 완료이면 해제")
	void delayedPredecessorImpact() throws Exception {
		final long predecessor = createTask("선행", developer.getId(), "2026-12-01", "2026-12-10");
		final long successor = createTask("후행", developer.getId(), "2026-12-11", "2026-12-20");
		replace(pm, successor, List.of(predecessor));
		clock.setInstant(LocalDateTime.of(2026, 12, 12, 0, 0).atZone(BusinessCalendar.ZONE).toInstant());
		pm = loginAs(creator);

		JsonNode item = readResponse(getAs(pm, PREDECESSORS, projectId, successor)).get("predecessors").get(0);
		assertThat(item.get("delayed").asBoolean()).isTrue();
		assertThat(item.get("delayDays").asLong()).isEqualTo(2);
		assertThat(item.get("impactsSuccessor").asBoolean()).isTrue();

		jdbcTemplate.update("UPDATE wbs_tasks SET status = 'DONE' WHERE id = ?", predecessor);
		item = readResponse(getAs(pm, PREDECESSORS, projectId, successor)).get("predecessors").get(0);
		assertThat(item.get("delayed").asBoolean()).isFalse();
		assertThat(item.get("delayDays").asLong()).isZero();
		assertThat(item.get("impactsSuccessor").asBoolean()).isFalse();
	}

	@Test
	@DisplayName("권한: 조회 가능자는 읽고, PM은 전체 변경, 개발자는 본인 담당 후행만 변경한다")
	void permissions() throws Exception {
		final long predecessor = createTask("선행", creator.getId(), "2026-12-01", "2026-12-10");
		final long own = createTask("본인 업무", developer.getId(), "2026-12-11", "2026-12-20");
		final long other = createTask("다른 업무", creator.getId(), "2026-12-11", "2026-12-20");
		final Cookie developerCookie = loginAs(developer);

		assertThat(ids(replace(developerCookie, own, List.of(predecessor)))).containsExactly(predecessor);
		expectError(putJson(developerCookie, body("predecessorTaskIds", List.of(predecessor)), PREDECESSORS,
			projectId, other), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		expectError(putJson(loginAs(businessOwner), body("predecessorTaskIds", List.of(predecessor)), PREDECESSORS,
			projectId, own), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);

		for (final Cookie viewer : List.of(loginAs(businessOwner), loginAs(outsider), loginAs(itExecutive))) {
			assertThat(ids(readResponse(getAs(viewer, PREDECESSORS, projectId, own)))).containsExactly(predecessor);
		}
		assertThat(dependencyRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("순환·참조 검증: 자기 참조, 간접 순환, 다른 프로젝트·없는 작업을 거부하고 기존 관계를 보존한다")
	void rejectsCyclesAndInvalidReferences() throws Exception {
		final long a = createTask("A", creator.getId(), "2026-12-01", "2026-12-02");
		final long b = createTask("B", creator.getId(), "2026-12-03", "2026-12-04");
		final long c = createTask("C", creator.getId(), "2026-12-05", "2026-12-06");
		replace(pm, b, List.of(a));
		replace(pm, c, List.of(b));

		expectError(putJson(pm, body("predecessorTaskIds", List.of(c)), PREDECESSORS, projectId, a),
			ErrorCode.WBS_DEPENDENCY_CYCLE);
		expectError(putJson(pm, body("predecessorTaskIds", List.of(a)), PREDECESSORS, projectId, a),
			ErrorCode.INVALID_WBS_DEPENDENCY);

		final long otherProjectId = createProject(validCreateBody("DEP-OTHER"));
		final long foreign = createTaskIn(otherProjectId, "다른 프로젝트");
		expectError(putJson(pm, body("predecessorTaskIds", List.of(foreign)), PREDECESSORS, projectId, c),
			ErrorCode.INVALID_WBS_DEPENDENCY);
		expectError(putJson(pm, body("predecessorTaskIds", List.of(999_999L)), PREDECESSORS, projectId, c),
			ErrorCode.INVALID_WBS_DEPENDENCY);
		assertThat(dependencyRepository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("입력·자원 경계: null·음수·201개는 400, 없는 후행과 다른 프로젝트 후행은 404, WBS 기능 꺼짐은 404")
	void requestBoundaries() throws Exception {
		final long task = createTask("작업", creator.getId(), "2026-12-01", "2026-12-02");
		putJson(pm, body("predecessorTaskIds", null), PREDECESSORS, projectId, task)
			.andExpect(status().isBadRequest());
		putJson(pm, body("predecessorTaskIds", List.of(-1L)), PREDECESSORS, projectId, task)
			.andExpect(status().isBadRequest());
		putJson(pm, body("predecessorTaskIds", Collections.singletonList(null)), PREDECESSORS, projectId, task)
			.andExpect(status().isBadRequest());
		final List<Long> tooMany = new ArrayList<>();
		for (long id = 1; id <= 201; id++) {
			tooMany.add(id);
		}
		putJson(pm, body("predecessorTaskIds", tooMany), PREDECESSORS, projectId, task)
			.andExpect(status().isBadRequest());
		expectError(getAs(pm, PREDECESSORS, projectId, 999_999L), ErrorCode.WBS_TASK_NOT_FOUND);

		final long otherProjectId = createProject(validCreateBody("DEP-BOUNDARY"));
		final long foreign = createTaskIn(otherProjectId, "다른 프로젝트 후행");
		expectError(getAs(pm, PREDECESSORS, projectId, foreign), ErrorCode.WBS_TASK_NOT_FOUND);

		final Map<String, Object> noWbs = validCreateBody("DEP-NOWBS");
		noWbs.put("features", List.of("KANBAN"));
		final long noWbsId = createProject(noWbs);
		expectError(getAs(pm, PREDECESSORS, noWbsId, 999_999L), ErrorCode.FEATURE_DISABLED);
	}

	private long createTask(
		final String name,
		final Long assigneeUserId,
		final String startDate,
		final String endDate
	) throws Exception {
		return readResponse(postJson(TASKS.replace("{projectId}", String.valueOf(projectId)), pm,
			taskBody(name, assigneeUserId, startDate, endDate))).get("taskId").asLong();
	}

	private long createTaskIn(final long targetProjectId, final String name) throws Exception {
		return readResponse(postJson(TASKS.replace("{projectId}", String.valueOf(targetProjectId)), pm,
			taskBody(name, null, "2026-12-01", "2026-12-02"))).get("taskId").asLong();
	}

	private JsonNode replace(final Cookie cookie, final long taskId, final List<Long> predecessorIds) throws Exception {
		return readResponse(putJson(cookie, body("predecessorTaskIds", predecessorIds), PREDECESSORS, projectId, taskId));
	}

	private static Map<String, Object> taskBody(
		final String name,
		final Long assigneeUserId,
		final String startDate,
		final String endDate
	) {
		return body("parentTaskId", null, "name", name, "description", null, "assigneeUserId", assigneeUserId,
			"startDate", startDate, "endDate", endDate, "estimatedEffort", null);
	}

	private static List<Long> ids(final JsonNode response) {
		return response.get("predecessors").valueStream().map(item -> item.get("id").asLong()).toList();
	}
}
