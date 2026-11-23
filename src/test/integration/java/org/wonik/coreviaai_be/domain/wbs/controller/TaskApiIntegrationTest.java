package org.wonik.coreviaai_be.domain.wbs.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.project.controller.ProjectApiTestSupport;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskHistory;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskActivityType;
import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskPriority;
import org.wonik.coreviaai_be.domain.wbs.entity.type.TaskType;
import org.wonik.coreviaai_be.domain.wbs.repository.TaskHistoryRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * FR-TSK-01 업무 CRUD를 HTTP 경계에서 검증한다: 업무 상세 조회, 상세 필드(유형·우선순위·완료 조건) 수정, 변경 이력
 * (WBS 등록·수정·이동·삭제 경로 포함), 권한 매트릭스(3.3 TSK-01), V11 마이그레이션 기본값.
 * <p>
 * 준비: 전사 공개 폭포수 프로젝트(2026-12-01 ~ 2027-03-31, WBS 기능 사용). 멤버 creator(PM)·developer(개발자)·
 * qa(QA)·businessOwner(현업 담당자). outsider는 멤버가 아닌 같은 부서 사용자(전사 공개라 조회 가능),
 * itExecutive는 주관 부서를 소속 범위로 둔 경영진.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskApiIntegrationTest extends ProjectApiTestSupport {

	private static final String WBS_TASKS = PROJECTS + "/{projectId}/wbs/tasks";
	private static final String WBS_TASK = WBS_TASKS + "/{taskId}";
	private static final String TASK = PROJECTS + "/{projectId}/tasks/{taskId}";
	private static final String HISTORIES = TASK + "/histories";
	private static final String PROGRESS = TASK + "/progress";

	@Autowired
	private WbsTaskRepository taskRepository;
	@Autowired
	private TaskHistoryRepository historyRepository;

	private User qa;
	private long projectId;
	private Cookie pm;

	@BeforeEach
	void createProject() throws Exception {
		qa = saveUser("qa01", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		final Map<String, Object> body = validCreateBody("PUR-2026");
		body.put("actors", List.of("PM", "BUSINESS_OWNER", "DEVELOPER", "QA"));
		body.put("members", List.of(
			member(developer.getId(), "DEVELOPER"),
			member(qa.getId(), "QA"),
			member(businessOwner.getId(), "BUSINESS_OWNER")));
		projectId = createProject(body);
		pm = loginAs(creator);
	}

	// ---- 등록·상세 조회 ----

	@Test
	@DisplayName("진척률: 하위작업 입력은 공수 가중 평균으로 상위·단계·프로젝트에 집계되고 이력과 낙관적 잠금을 적용한다")
	void updateAndAggregateProgress() throws Exception {
		final long parentId = createTask(pm, null, "상위", null);
		final long firstId = readResponse(postJson(url(WBS_TASKS), pm,
			body("parentTaskId", parentId, "name", "첫째", "assigneeUserId", developer.getId(),
				"startDate", "2026-12-01", "endDate", "2026-12-03", "estimatedEffort", 1.0)))
			.get("taskId").asLong();
		final long secondId = readResponse(postJson(url(WBS_TASKS), pm,
			body("parentTaskId", parentId, "name", "둘째", "assigneeUserId", qa.getId(),
				"startDate", "2026-12-04", "endDate", "2026-12-08", "estimatedEffort", 3.0)))
			.get("taskId").asLong();

		readResponse(patchJson(pm, body("progress", 100, "version", version(firstId)),
			PROGRESS, projectId, firstId));
		final long staleVersion = version(secondId);
		readResponse(patchJson(pm, body("progress", 50, "version", staleVersion),
			PROGRESS, projectId, secondId));

		assertThat(detail(pm, parentId).get("progress").asInt()).isEqualTo(63);
		assertThat(historyItems(pm, parentId).valueStream()
			.map(item -> item.get("activityType").asString()).toList()).contains("PROGRESS_CHANGED");
		assertThat(historyItems(pm, firstId).get(1).get("after").get("progress").asInt()).isEqualTo(100);
		assertThat(readResponse(getAs(pm, PROJECTS + "/{id}", projectId)).get("progress").asInt()).isEqualTo(63);
		assertThat(phasesOf(pm, projectId).get(0).get("progress").asInt()).isEqualTo(63);

		expectError(patchJson(pm, body("progress", 40, "version", staleVersion),
			PROGRESS, projectId, secondId), ErrorCode.CONCURRENT_MODIFICATION);
		expectError(patchJson(pm, body("progress", 80, "version", version(parentId)),
			PROGRESS, projectId, parentId), ErrorCode.WBS_PROGRESS_DERIVED);
		patchJson(pm, body("progress", -1, "version", version(firstId)), PROGRESS, projectId, firstId)
			.andExpect(status().isBadRequest());
		patchJson(pm, body("progress", 101, "version", version(firstId)), PROGRESS, projectId, firstId)
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("진척률 권한: 담당 개발자·QA와 PM만 수정하고 비담당자·현업 담당자는 거부한다")
	void progressPermissions() throws Exception {
		final long taskId = createTask(pm, null, "개발", developer.getId());
		readResponse(patchJson(loginAs(developer), body("progress", 30, "version", version(taskId)),
			PROGRESS, projectId, taskId));
		assertThat(detail(pm, taskId).get("progress").asInt()).isEqualTo(30);

		expectError(patchJson(loginAs(qa), body("progress", 40, "version", version(taskId)),
			PROGRESS, projectId, taskId), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		expectError(patchJson(loginAs(businessOwner), body("progress", 40, "version", version(taskId)),
			PROGRESS, projectId, taskId), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
	}

	@Test
	@DisplayName("등록·상세: 상세 필드를 비우면 TASK·MEDIUM으로, 지정하면 그 값으로 등록되고 상세·트리에 보이며 CREATED 이력이 남는다")
	void createAndFind() throws Exception {
		final long plain = createTask(pm, null, "설계", developer.getId());
		final long defect = readResponse(postJson(url(WBS_TASKS), pm, body("parentTaskId", null, "name", "로그인 오류",
			"assigneeUserId", qa.getId(), "startDate", "2026-12-01", "endDate", "2026-12-03", "estimatedEffort", 1.5,
			"taskType", "DEFECT", "priority", "HIGH", "completionCriteria", "  재현 절차로 다시 확인  ")))
			.get("taskId").asLong();

		final JsonNode plainDetail = detail(pm, plain);
		assertThat(plainDetail.get("taskType").asString()).isEqualTo("TASK");
		assertThat(plainDetail.get("priority").asString()).isEqualTo("MEDIUM");
		assertThat(plainDetail.get("completionCriteria").isNull()).isTrue();
		assertThat(plainDetail.get("completedAt").isNull()).isTrue();
		assertThat(plainDetail.get("projectId").asLong()).isEqualTo(projectId);
		assertThat(plainDetail.get("assignee").get("name").asString()).isEqualTo(developer.getName());
		assertThat(plainDetail.get("createdBy").get("id").asLong()).isEqualTo(creator.getId());
		assertThat(plainDetail.get("createdBy").get("name").asString()).isEqualTo(creator.getName());
		assertThat(plainDetail.get("status").asString()).isEqualTo("TODO");
		assertThat(plainDetail.get("version").asLong()).isZero();

		final JsonNode defectDetail = detail(pm, defect);
		assertThat(defectDetail.get("taskType").asString()).isEqualTo("DEFECT");
		assertThat(defectDetail.get("priority").asString()).isEqualTo("HIGH");
		assertThat(defectDetail.get("completionCriteria").asString()).isEqualTo("재현 절차로 다시 확인");
		assertThat(defectDetail.get("estimatedEffort").decimalValue()).isEqualByComparingTo("1.5");

		final JsonNode tree = readResponse(getAs(pm, WBS_TASKS, projectId)).get("tasks");
		assertThat(tree.get(1).get("taskType").asString()).isEqualTo("DEFECT");
		assertThat(tree.get(1).get("priority").asString()).isEqualTo("HIGH");

		final JsonNode created = historyItems(pm, defect).get(0);
		assertThat(created.get("activityType").asString()).isEqualTo("CREATED");
		assertThat(created.get("before").isNull()).isTrue();
		assertThat(created.get("after").get("name").asString()).isEqualTo("로그인 오류");
		assertThat(created.get("after").get("assigneeUserId").asLong()).isEqualTo(qa.getId());
		assertThat(created.get("after").get("taskType").asString()).isEqualTo("DEFECT");
		assertThat(created.get("after").get("source").asString()).isEqualTo("MANUAL");
		assertThat(created.get("after").get("estimatedEffort").decimalValue()).isEqualByComparingTo("1.5");
		assertThat(created.get("actor").get("id").asLong()).isEqualTo(creator.getId());
		assertThat(created.get("actor").get("name").asString()).isEqualTo(creator.getName());
		assertThat(created.get("occurredAt").asString()).isNotBlank();
	}

	@Test
	@DisplayName("조회 권한: 멤버·현업 담당자·공개 범위 열람자·소속 범위 경영진은 상세·이력 조회, 볼 수 없거나 다른 프로젝트 업무·관리자는 404")
	void readPermissions() throws Exception {
		final long taskId = createTask(pm, null, "설계", null);

		for (final User viewer : List.of(developer, qa, businessOwner, outsider, itExecutive)) {
			final Cookie cookie = loginAs(viewer);
			assertThat(detail(cookie, taskId).get("name").asString()).isEqualTo("설계");
			assertThat(historyItems(cookie, taskId)).hasSize(1);
		}

		final Map<String, Object> membersOnly = validCreateBody("MEM-2026");
		membersOnly.put("visibility", "MEMBERS_ONLY");
		final long membersOnlyId = createProject(membersOnly);
		final long hiddenTask = readResponse(postJson("/api/projects/" + membersOnlyId + "/wbs/tasks", pm,
			taskBody(null, "비공개", null))).get("taskId").asLong();
		expectError(getAs(loginAs(outsider), TASK, membersOnlyId, hiddenTask), ErrorCode.PROJECT_NOT_FOUND);
		expectError(getAs(loginAs(outsider), HISTORIES, membersOnlyId, hiddenTask), ErrorCode.PROJECT_NOT_FOUND);
		expectError(getAs(loginAs(salesExecutive), TASK, membersOnlyId, hiddenTask), ErrorCode.PROJECT_NOT_FOUND);

		// 다른 프로젝트의 업무 ID로 접근(IDOR)하면 업무 404
		expectError(getAs(pm, TASK, projectId, hiddenTask), ErrorCode.WBS_TASK_NOT_FOUND);
		expectError(getAs(pm, HISTORIES, projectId, hiddenTask), ErrorCode.WBS_TASK_NOT_FOUND);
		expectError(patchJson(pm, attributesBody("DEFECT", "HIGH", null, 0L), TASK, projectId, hiddenTask),
			ErrorCode.WBS_TASK_NOT_FOUND);
		expectError(getAs(pm, TASK, projectId, 999_999L), ErrorCode.WBS_TASK_NOT_FOUND);

		// WBS·칸반을 모두 끈 프로젝트만 기능 404(둘 중 하나만 켜져 있어도 업무 상세를 연다, SCRUM-110)
		final Map<String, Object> noTaskModule = validCreateBody("NOWBS-2026");
		noTaskModule.put("features", List.of("BOARD"));
		final long noTaskModuleId = createProject(noTaskModule);
		expectError(getAs(pm, TASK, noTaskModuleId, taskId), ErrorCode.FEATURE_DISABLED);

		saveUser("admin01", USER_PASSWORD, SystemRole.SYSTEM_ADMIN, null);
		expectError(getAs(loginOk("admin01", USER_PASSWORD), TASK, projectId, taskId), ErrorCode.NOT_FOUND);
	}

	// ---- 상세 필드 수정 ----

	@Test
	@DisplayName("권한 매트릭스: PM은 모든 업무, 개발자·QA는 본인 담당 업무만 상세 필드 수정, 현업 담당자·경영진·비멤버는 403")
	void updatePermissions() throws Exception {
		final long devTask = createTask(pm, null, "개발자 작업", developer.getId());
		final long qaTask = createTask(pm, null, "QA 작업", qa.getId());
		final long pmTask = createTask(pm, null, "PM 작업", null);

		updateAttributes(pm, pmTask, "DEFECT", "LOW", "PM이 정함");
		updateAttributes(loginAs(developer), devTask, "TASK", "HIGH", "개발자가 정함");
		updateAttributes(loginAs(qa), qaTask, "DEFECT", "HIGH", null);
		assertThat(taskRepository.findById(devTask).orElseThrow().getPriority()).isEqualTo(TaskPriority.HIGH);
		assertThat(taskRepository.findById(qaTask).orElseThrow().getTaskType()).isEqualTo(TaskType.DEFECT);

		// 개발자·QA는 남의 업무(담당 없음·다른 사람 담당)를 고칠 수 없다
		expectError(patchJson(loginAs(developer), attributesBody("DEFECT", "LOW", null, version(qaTask)), TASK,
			projectId, qaTask), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		expectError(patchJson(loginAs(qa), attributesBody("DEFECT", "LOW", null, version(pmTask)), TASK,
			projectId, pmTask), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		// 현업 담당자·경영진·멤버 아닌 열람자는 조회만
		for (final User viewer : List.of(businessOwner, itExecutive, outsider)) {
			expectError(patchJson(loginAs(viewer), attributesBody("DEFECT", "LOW", null, version(devTask)), TASK,
				projectId, devTask), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		}
		assertThat(taskRepository.findById(devTask).orElseThrow().getTaskType()).isEqualTo(TaskType.TASK);
	}

	@Test
	@DisplayName("상세 필드 수정: 바뀐 필드만 전·후 값으로 DETAILS_CHANGED 이력, 같은 값 재전송은 version이 달라도 200(이력 없음), 낡은 version은 409")
	void updateAttributesHistoryAndLocking() throws Exception {
		final long taskId = createTask(pm, null, "설계", developer.getId());

		final JsonNode updated = updateAttributes(pm, taskId, "DEFECT", "MEDIUM", "리뷰 승인");
		assertThat(updated.get("taskType").asString()).isEqualTo("DEFECT");
		assertThat(updated.get("completionCriteria").asString()).isEqualTo("리뷰 승인");
		assertThat(updated.get("version").asLong()).isEqualTo(1);

		final JsonNode changed = historyItems(pm, taskId).get(1);
		assertThat(changed.get("activityType").asString()).isEqualTo("DETAILS_CHANGED");
		assertThat(fieldNames(changed.get("before"))).containsExactly("taskType", "completionCriteria");
		assertThat(changed.get("before").get("taskType").asString()).isEqualTo("TASK");
		assertThat(changed.get("before").get("completionCriteria").isNull()).isTrue();
		assertThat(changed.get("after").get("taskType").asString()).isEqualTo("DEFECT");
		assertThat(changed.get("after").get("completionCriteria").asString()).isEqualTo("리뷰 승인");

		// 같은 값 재전송: 낡은 version이어도 200, 이력·version 그대로
		readResponse(patchJson(pm, attributesBody("DEFECT", "MEDIUM", " 리뷰 승인 ", 0L), TASK, projectId, taskId));
		assertThat(historyItems(pm, taskId)).hasSize(2);
		assertThat(version(taskId)).isEqualTo(1);

		// 다른 값 + 낡은 version: 409, 바뀌지 않고 이력도 없음
		expectError(patchJson(pm, attributesBody("TASK", "LOW", null, 0L), TASK, projectId, taskId),
			ErrorCode.CONCURRENT_MODIFICATION);
		assertThat(taskRepository.findById(taskId).orElseThrow().getPriority()).isEqualTo(TaskPriority.MEDIUM);
		assertThat(historyItems(pm, taskId)).hasSize(2);

		// WBS 기본 필드 수정과 같은 version을 공유한다: 상세 수정 뒤 WBS 수정에 옛 version을 보내면 409
		expectError(patchJson(pm, wbsUpdateBody("새 이름", developer.getId(), "2026-12-01", "2026-12-10", 0L), WBS_TASK,
			projectId, taskId), ErrorCode.CONCURRENT_MODIFICATION);

		// 완료 조건 비우기(공백만 보내도 지움)
		updateAttributes(pm, taskId, "DEFECT", "MEDIUM", "   ");
		assertThat(taskRepository.findById(taskId).orElseThrow().getCompletionCriteria()).isNull();
	}

	@Test
	@DisplayName("경계값: 완료 조건 2000자는 저장, 2001자·유형·우선순위 누락·허용 밖 값·version 누락·음수는 400")
	void attributeBoundaries() throws Exception {
		final long taskId = createTask(pm, null, "설계", null);

		updateAttributes(pm, taskId, "TASK", "LOW", "가".repeat(2000));
		assertThat(taskRepository.findById(taskId).orElseThrow().getCompletionCriteria()).hasSize(2000);

		final long v = version(taskId);
		patchJson(pm, attributesBody("TASK", "LOW", "가".repeat(2001), v), TASK, projectId, taskId)
			.andExpect(status().isBadRequest());
		patchJson(pm, attributesBody(null, "LOW", null, v), TASK, projectId, taskId).andExpect(status().isBadRequest());
		patchJson(pm, attributesBody("TASK", null, null, v), TASK, projectId, taskId).andExpect(status().isBadRequest());
		patchJson(pm, attributesBody("BUG", "LOW", null, v), TASK, projectId, taskId).andExpect(status().isBadRequest());
		patchJson(pm, attributesBody("TASK", "URGENT", null, v), TASK, projectId, taskId)
			.andExpect(status().isBadRequest());
		patchJson(pm, attributesBody("TASK", "LOW", null, null), TASK, projectId, taskId)
			.andExpect(status().isBadRequest());
		patchJson(pm, attributesBody("TASK", "LOW", null, -1L), TASK, projectId, taskId)
			.andExpect(status().isBadRequest());
		// 등록 때도 같은 규칙
		final Map<String, Object> tooLong = taskBody(null, "작업", null);
		tooLong.put("completionCriteria", "가".repeat(2001));
		postJson(url(WBS_TASKS), pm, tooLong).andExpect(status().isBadRequest());
		final Map<String, Object> badType = taskBody(null, "작업", null);
		badType.put("taskType", "BUG");
		postJson(url(WBS_TASKS), pm, badType).andExpect(status().isBadRequest());

		assertThat(taskRepository.count()).isEqualTo(1);
		assertThat(historyRepository.findAllByTaskIdOrderByIdAsc(taskId)).hasSize(2);
	}

	// ---- WBS 경로의 변경 이력 ----

	@Test
	@DisplayName("배정·해제: PM이 담당자를 배정·변경·해제하면 ASSIGNED·ASSIGNED·UNASSIGNED 이력(전·후 담당자)이 남고, 개발자는 배정을 바꿀 수 없다")
	void assignAndUnassign() throws Exception {
		final long taskId = createTask(pm, null, "설계", null);

		wbsUpdate(pm, taskId, "설계", developer.getId(), "2026-12-01", "2026-12-10");
		wbsUpdate(pm, taskId, "설계", qa.getId(), "2026-12-01", "2026-12-10");
		expectError(patchJson(loginAs(qa), wbsUpdateBody("설계", developer.getId(), "2026-12-01", "2026-12-10",
			version(taskId)), WBS_TASK, projectId, taskId), ErrorCode.PROJECT_PM_ONLY);
		wbsUpdate(pm, taskId, "설계", null, "2026-12-01", "2026-12-10");

		final JsonNode items = historyItems(pm, taskId);
		assertThat(activityTypes(items)).containsExactly("CREATED", "ASSIGNED", "ASSIGNED", "UNASSIGNED");
		assertThat(items.get(1).get("before").get("assigneeUserId").isNull()).isTrue();
		assertThat(items.get(1).get("after").get("assigneeUserId").asLong()).isEqualTo(developer.getId());
		assertThat(items.get(2).get("before").get("assigneeUserId").asLong()).isEqualTo(developer.getId());
		assertThat(items.get(2).get("after").get("assigneeUserId").asLong()).isEqualTo(qa.getId());
		assertThat(items.get(3).get("before").get("assigneeUserId").asLong()).isEqualTo(qa.getId());
		assertThat(items.get(3).get("after").get("assigneeUserId").isNull()).isTrue();
		// 배정된 담당자는 상세에 바로 보인다(내 TODO는 FR-PSN-01이 wbs_tasks.assignee_user_id로 조회)
		assertThat(detail(pm, taskId).get("assignee").isNull()).isTrue();
	}

	@Test
	@DisplayName("일정·상세 변경: 하위작업 기간을 바꾸면 하위작업과 산출된 상위 작업에 DATE_CHANGED, 이름·공수 변경은 DETAILS_CHANGED, 실패한 요청은 이력을 남기지 않는다")
	void scheduleAndDetailHistories() throws Exception {
		final long parent = createTask(pm, null, "설계", null);
		final long subtask = readResponse(postJson(url(WBS_TASKS), pm, taskBody(parent, "API 설계", developer.getId())))
			.get("taskId").asLong();
		// 하위작업 등록으로 상위 기간이 12-01~12-10 → 12-01~12-05로 산출
		assertThat(activityTypes(historyItems(pm, parent))).containsExactly("CREATED", "DATE_CHANGED");

		final Cookie dev = loginAs(developer);
		readResponse(patchJson(dev, body("name", "API 상세 설계", "description", null, "assigneeUserId",
			developer.getId(), "startDate", "2026-12-02", "endDate", "2026-12-20", "estimatedEffort", 2.5,
			"version", version(subtask)), WBS_TASK, projectId, subtask));

		final JsonNode subItems = historyItems(pm, subtask);
		assertThat(activityTypes(subItems)).containsExactly("CREATED", "DATE_CHANGED", "DETAILS_CHANGED");
		assertThat(subItems.get(1).get("before").get("endDate").asString()).isEqualTo("2026-12-05");
		assertThat(subItems.get(1).get("after").get("startDate").asString()).isEqualTo("2026-12-02");
		assertThat(subItems.get(1).get("after").get("endDate").asString()).isEqualTo("2026-12-20");
		assertThat(subItems.get(1).get("actor").get("id").asLong()).isEqualTo(developer.getId());
		assertThat(fieldNames(subItems.get(2).get("after"))).containsExactly("name", "estimatedEffort");
		assertThat(subItems.get(2).get("before").get("estimatedEffort").isNull()).isTrue();
		assertThat(subItems.get(2).get("after").get("estimatedEffort").decimalValue()).isEqualByComparingTo("2.5");

		final JsonNode parentItems = historyItems(pm, parent);
		assertThat(activityTypes(parentItems)).containsExactly("CREATED", "DATE_CHANGED", "DATE_CHANGED");
		assertThat(parentItems.get(2).get("after").get("startDate").asString()).isEqualTo("2026-12-02");
		assertThat(parentItems.get(2).get("after").get("endDate").asString()).isEqualTo("2026-12-20");

		// 프로젝트 기간 밖으로 바꾸려다 실패하면(400) 변경·이력 모두 없음(같은 트랜잭션)
		expectError(patchJson(pm, wbsUpdateBody("API 상세 설계", developer.getId(), "2026-11-01", "2026-12-20",
			version(subtask)), WBS_TASK, projectId, subtask), ErrorCode.INVALID_WBS_SCHEDULE);
		assertThat(historyItems(pm, subtask)).hasSize(3);
	}

	@Test
	@DisplayName("이동·삭제: 이동은 MOVED(부모·순서 전·후), 이력이 있어도 삭제는 막히지 않고 DELETED 이력이 남으며 이력 조회는 404")
	void moveAndDelete() throws Exception {
		final long first = createTask(pm, null, "설계", null);
		final long second = createTask(pm, null, "개발", null);

		readResponse(putJson(pm, body("parentTaskId", first, "index", 0, "version", version(second)),
			WBS_TASK + "/position", projectId, second));
		final JsonNode moved = historyItems(pm, second).get(1);
		assertThat(moved.get("activityType").asString()).isEqualTo("MOVED");
		assertThat(moved.get("before").get("parentTaskId").isNull()).isTrue();
		assertThat(moved.get("before").get("sortOrder").asInt()).isEqualTo(2);
		assertThat(moved.get("after").get("parentTaskId").asLong()).isEqualTo(first);
		assertThat(moved.get("after").get("sortOrder").asInt()).isEqualTo(1);
		// 형제 순서 재정렬·상위 기간 산출(같은 기간이라 변화 없음)은 이력을 늘리지 않는다
		assertThat(activityTypes(historyItems(pm, first))).containsExactly("CREATED");

		// 개발자는 삭제 불가(PM만), PM은 이력이 있어도 삭제 가능
		expectError(deleteAs(loginAs(developer), WBS_TASK, projectId, second), ErrorCode.PROJECT_PM_ONLY);
		readResponse(deleteAs(pm, WBS_TASK, projectId, second));
		assertThat(taskRepository.findById(second)).isEmpty();

		final List<TaskHistory> kept = historyRepository.findAllByTaskIdOrderByIdAsc(second);
		assertThat(kept).extracting(TaskHistory::getActivityType)
			.containsExactly(TaskActivityType.CREATED, TaskActivityType.MOVED, TaskActivityType.DELETED);
		final TaskHistory deleted = kept.get(2);
		assertThat(deleted.getAfterValue()).isNull();
		assertThat(deleted.getBeforeValue()).contains("\"name\":\"개발\"", "\"parentTaskId\":" + first, "\"status\":\"TODO\"");
		assertThat(deleted.getActorUserId()).isEqualTo(creator.getId());
		// JSONB 컬럼에는 문자열이 아닌 JSON 객체로 저장된다
		assertThat(jdbcTemplate.queryForObject("SELECT CAST(before_value AS VARCHAR) FROM task_histories WHERE id = ?",
			String.class, deleted.getId())).startsWith("{");
		expectError(getAs(pm, HISTORIES, projectId, second), ErrorCode.WBS_TASK_NOT_FOUND);
		expectError(getAs(pm, TASK, projectId, second), ErrorCode.WBS_TASK_NOT_FOUND);
	}

	// ---- 이력 조회 ----

	@Test
	@DisplayName("이력 페이지: 시간순(id 오름차순) 커서 페이지, size 경계(1·100 허용, 0·101 400), 잘못된 커서 400")
	void historyPaging() throws Exception {
		final long taskId = createTask(pm, null, "설계", null);
		updateAttributes(pm, taskId, "DEFECT", "MEDIUM", null);
		updateAttributes(pm, taskId, "DEFECT", "HIGH", null);
		updateAttributes(pm, taskId, "DEFECT", "LOW", null);

		final JsonNode first = readResponse(getAs(pm, HISTORIES + "?size=2", projectId, taskId));
		assertThat(activityTypes(first.get("items"))).containsExactly("CREATED", "DETAILS_CHANGED");
		assertThat(first.get("hasNext").asBoolean()).isTrue();
		final JsonNode second = readResponse(getAs(pm, HISTORIES + "?size=2&cursor=" + first.get("nextCursor").asString(),
			projectId, taskId));
		assertThat(second.get("items")).hasSize(2);
		assertThat(second.get("items").get(1).get("after").get("priority").asString()).isEqualTo("LOW");
		assertThat(second.get("hasNext").asBoolean()).isFalse();
		assertThat(second.get("nextCursor").isNull()).isTrue();
		assertThat(second.get("items").get(0).get("id").asLong())
			.isGreaterThan(first.get("items").get(1).get("id").asLong());

		readResponse(getAs(pm, HISTORIES + "?size=1", projectId, taskId));
		readResponse(getAs(pm, HISTORIES + "?size=100", projectId, taskId));
		getAs(pm, HISTORIES + "?size=0", projectId, taskId).andExpect(status().isBadRequest());
		getAs(pm, HISTORIES + "?size=101", projectId, taskId).andExpect(status().isBadRequest());
		getAs(pm, HISTORIES + "?cursor=not-a-cursor", projectId, taskId).andExpect(status().isBadRequest());
	}

	// ---- 마이그레이션 ----

	@Test
	@DisplayName("V11: 상세 컬럼 없이 넣은 기존 행은 TASK·MEDIUM 기본값으로 읽히고, CHECK 밖 유형·우선순위는 DB가 거부한다")
	void migrationDefaults() throws Exception {
		jdbcTemplate.update("""
			INSERT INTO wbs_tasks (project_id, sort_order, name, start_date, end_date, progress, status, source,
			    created_by, version, created_at, updated_at)
			VALUES (?, 1, '기존 작업', DATE '2026-12-01', DATE '2026-12-05', 0, 'TODO', 'MANUAL', ?, 0,
			    CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
			""", projectId, creator.getId());
		final WbsTask legacy = taskRepository.findAllByProjectIdOrderBySortOrderAscIdAsc(projectId).get(0);

		assertThat(legacy.getTaskType()).isEqualTo(TaskType.TASK);
		assertThat(legacy.getPriority()).isEqualTo(TaskPriority.MEDIUM);
		assertThat(legacy.getCompletionCriteria()).isNull();
		assertThat(legacy.getCompletedAt()).isNull();
		assertThat(detail(pm, legacy.getId()).get("taskType").asString()).isEqualTo("TASK");
		// 기존 작업은 과거 이력이 없다(이 버전 이후 변경부터 기록)
		assertThat(historyItems(pm, legacy.getId())).isEmpty();

		assertThatThrownBy(() -> jdbcTemplate.update("UPDATE wbs_tasks SET task_type = 'BUG' WHERE id = ?",
			legacy.getId())).isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> jdbcTemplate.update("UPDATE wbs_tasks SET priority = 'URGENT' WHERE id = ?",
			legacy.getId())).isInstanceOf(DataIntegrityViolationException.class);
	}

	// ---- helpers ----

	private String url(final String template) {
		return template.replace("{projectId}", String.valueOf(projectId));
	}

	private long createTask(final Cookie cookie, final Long parentTaskId, final String name, final Long assigneeUserId)
		throws Exception {
		return readResponse(postJson(url(WBS_TASKS), cookie, taskBody(parentTaskId, name, assigneeUserId)))
			.get("taskId").asLong();
	}

	private JsonNode detail(final Cookie cookie, final long taskId) throws Exception {
		return readResponse(getAs(cookie, TASK, projectId, taskId));
	}

	private JsonNode historyItems(final Cookie cookie, final long taskId) throws Exception {
		return readResponse(getAs(cookie, HISTORIES + "?size=100", projectId, taskId)).get("items");
	}

	private long version(final long taskId) {
		return taskRepository.findById(taskId).orElseThrow().getVersion();
	}

	private JsonNode updateAttributes(
		final Cookie cookie,
		final long taskId,
		final String taskType,
		final String priority,
		final String completionCriteria
	) throws Exception {
		return readResponse(patchJson(cookie, attributesBody(taskType, priority, completionCriteria, version(taskId)),
			TASK, projectId, taskId));
	}

	private void wbsUpdate(
		final Cookie cookie,
		final long taskId,
		final String name,
		final Long assigneeUserId,
		final String start,
		final String end
	) throws Exception {
		readResponse(patchJson(cookie, wbsUpdateBody(name, assigneeUserId, start, end, version(taskId)), WBS_TASK,
			projectId, taskId));
	}

	private static Map<String, Object> taskBody(final Long parentTaskId, final String name, final Long assigneeUserId) {
		final boolean subtask = parentTaskId != null;
		return body("parentTaskId", parentTaskId, "name", name, "description", null, "assigneeUserId", assigneeUserId,
			"startDate", "2026-12-01", "endDate", subtask ? "2026-12-05" : "2026-12-10", "estimatedEffort", null);
	}

	private static Map<String, Object> attributesBody(
		final String taskType,
		final String priority,
		final String completionCriteria,
		final Long version
	) {
		return body("taskType", taskType, "priority", priority, "completionCriteria", completionCriteria,
			"version", version);
	}

	private static Map<String, Object> wbsUpdateBody(
		final String name,
		final Long assigneeUserId,
		final String start,
		final String end,
		final Long version
	) {
		return body("name", name, "description", null, "assigneeUserId", assigneeUserId, "startDate", start,
			"endDate", end, "estimatedEffort", null, "version", version);
	}

	private static List<String> activityTypes(final JsonNode items) {
		return items.valueStream().map(item -> item.get("activityType").asString()).toList();
	}

	private static List<String> fieldNames(final JsonNode node) {
		return node.propertyNames().stream().toList();
	}
}
