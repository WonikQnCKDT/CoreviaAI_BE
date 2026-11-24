package org.wonik.coreviaai_be.domain.worklog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.project.controller.ProjectApiTestSupport;
import org.wonik.global.common.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorklogTaskApiIntegrationTest extends ProjectApiTestSupport {
    private long project;
    private long task;
    private Cookie owner;
    @BeforeEach void setupTasks() throws Exception {
        project = createProject(validCreateBody("LOG-01"));
        owner = loginAs(developer);
        task = addTask(null, "Task");
    }
    private long addTask(Long parent, String name) throws Exception {
        return readResponse(postJson("/api/projects/" + project + "/wbs/tasks", loginAs(creator),
            body("parentTaskId", parent, "name", name, "assigneeUserId", developer.getId(),
                "startDate", "2026-12-01", "endDate", "2026-12-03", "estimatedEffort", 1.0)))
            .get("taskId").asLong();
    }
    private long version(long id) {
        return jdbcTemplate.queryForObject("select version from wbs_tasks where id = ?", Long.class, id);
    }
    private JsonNode save(long id, int progress, boolean completed, int minutes) throws Exception {
        return readResponse(putJson(owner, body("projectId", project, "content", "shared task note",
            "progress", progress, "spentMinutes", minutes, "completed", completed,
            "taskVersion", version(id), "version", 0), "/api/me/worklogs/2026-12-01/tasks/{id}", id));
    }
    @Test void completionUpdatesTaskParentAndRetryDoesNotDoubleTime() throws Exception {
        long child = addTask(task, "Child");
        save(child, 100, true, 90);
        save(child, 100, true, 90);
        assertThat(jdbcTemplate.queryForObject("select progress from wbs_tasks where id = ?", Integer.class, task)).isEqualTo(100);
        assertThat(jdbcTemplate.queryForObject("select status from wbs_tasks where id = ?", String.class, child)).isEqualTo("DONE");
        assertThat(jdbcTemplate.queryForObject("select sum(spent_minutes) from worklog_tasks where task_id = ?", Long.class, child)).isEqualTo(90);
        assertThat(jdbcTemplate.queryForObject("select count(*) from worklog_tasks where task_id = ?", Integer.class, child)).isEqualTo(1);
    }
    @Test void pmSeesOnlyLinkedTaskContentAndOtherRolesCannotRead() throws Exception {
        readResponse(putJson(owner, body("content", "PRIVATE DAILY NOTE"), "/api/me/worklogs/2026-12-01"));
        save(task, 40, false, 60);
        String path = "/api/projects/{id}/worklogs?from=2026-12-01&to=2026-12-01";
        var result = readResponse(getAs(loginAs(creator), path, project));
        assertThat(result.toString()).contains("shared task note").doesNotContain("PRIVATE DAILY NOTE");
        expectError(getAs(loginAs(outsider), path, project), ErrorCode.PROJECT_PM_ONLY);
        expectError(getAs(owner, path, project), ErrorCode.PROJECT_PM_ONLY);
        expectError(getAs(loginAs(itExecutive), path, project), ErrorCode.PROJECT_PM_ONLY);
        long otherProject = createProject(validCreateBody("OTHER-01"));
        assertThat(readResponse(getAs(loginAs(creator), path, otherProject)).get("items").size()).isZero();
    }
    @Test void deniedAndStaleWritesDoNotCreateNotesOrChangeProgress() throws Exception {
        var request = body("projectId", project, "content", "attempt", "progress", 50,
            "spentMinutes", 30, "completed", false, "taskVersion", 999, "version", 0);
        expectError(putJson(owner, request, "/api/me/worklogs/2026-12-01/tasks/{id}", task), ErrorCode.CONCURRENT_MODIFICATION);
        expectError(putJson(loginAs(outsider), request, "/api/me/worklogs/2026-12-01/tasks/{id}", task), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
        assertThat(jdbcTemplate.queryForObject("select count(*) from worklog_tasks", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from worklogs", Integer.class)).isZero();
        request.put("spentMinutes", 1441);
        putJson(owner, request, "/api/me/worklogs/2026-12-01/tasks/{id}", task).andExpect(status().isBadRequest());
    }
    @Test void calendarReflectsTaskAssignmentDatesAndCompletionWithoutCopies() throws Exception {
        String path = "/api/me/calendar?from=2026-11-30T15:00:00Z&to=2026-12-04T15:00:00Z";
        var first = readResponse(getAs(owner, path)).get("items");
        assertThat(first.size()).isEqualTo(1);
        assertThat(first.get(0).get("startAt").asString()).isEqualTo("2026-11-30T15:00:00Z");
        assertThat(first.get(0).get("endAt").asString()).isEqualTo("2026-12-03T15:00:00Z");
        save(task, 100, true, 120);
        assertThat(readResponse(getAs(owner, path)).get("items").get(0).get("taskStatus").asString()).isEqualTo("DONE");
        assertThat(readResponse(getAs(owner, path)).get("items").get(0).get("spentMinutes").asLong()).isEqualTo(120);
        jdbcTemplate.update("update wbs_tasks set assignee_user_id = ? where id = ?", creator.getId(), task);
        assertThat(readResponse(getAs(owner, path)).get("items").size()).isZero();
        assertThat(readResponse(getAs(loginAs(creator), path)).get("items").size()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from personal_events", Integer.class)).isZero();
    }
}
