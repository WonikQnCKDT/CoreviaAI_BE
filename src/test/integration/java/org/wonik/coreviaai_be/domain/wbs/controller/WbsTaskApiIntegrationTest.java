package org.wonik.coreviaai_be.domain.wbs.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
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
import org.wonik.coreviaai_be.domain.wbs.entity.Milestone;
import org.wonik.coreviaai_be.domain.wbs.entity.MilestoneTask;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsDependency;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.coreviaai_be.domain.wbs.repository.MilestoneRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.MilestoneTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsDependencyRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.service.WbsTaskService;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditLog;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * FR-WBS-01 계층형 WBS를 HTTP 경계에서 검증한다.
 * <p>
 * 준비: 전사 공개 폭포수 프로젝트(2026-12-01 ~ 2027-03-31, WBS 기능 사용). 멤버 creator(PM)·developer(개발자)·
 * qa(QA)·businessOwner(현업 담당자). outsider는 멤버가 아닌 같은 부서 사용자(전사 공개라 조회 가능),
 * itExecutive는 주관 부서를 소속 범위로 둔 경영진.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WbsTaskApiIntegrationTest extends ProjectApiTestSupport {

	private static final String TASKS = PROJECTS + "/{projectId}/wbs/tasks";
	private static final String TASK = TASKS + "/{taskId}";
	private static final String POSITION = TASK + "/position";

	@Autowired
	private WbsTaskRepository taskRepository;
	@Autowired
	private WbsDependencyRepository dependencyRepository;
	@Autowired
	private MilestoneRepository milestoneRepository;
	@Autowired
	private MilestoneTaskRepository milestoneTaskRepository;

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

	// ---- 조회·등록 ----

	@Test
	@DisplayName("최상위 작업은 프로젝트 단계를 지정하고 하위작업은 상위 단계 상속, 다른 프로젝트 단계는 거부한다")
	void assignProjectPhase() throws Exception {
		final long phaseId = phasesOf(pm, projectId).get(1).get("id").asLong();
		final Map<String, Object> request = taskBody(null, "단계 지정 작업", null,
			"2026-12-01", "2026-12-10", 2.0);
		request.put("phaseId", phaseId);
		final JsonNode created = readResponse(postJson(url(TASKS), pm, request));
		final long parentId = created.get("taskId").asLong();
		assertThat(created.get("tasks").get(0).get("phaseId").asLong()).isEqualTo(phaseId);

		final long childId = createTask(pm, parentId, "하위작업", null, "2026-12-02", "2026-12-03");
		assertThat(node(childId).get("phaseId").asLong()).isEqualTo(phaseId);
		expectError(deleteAs(pm, PROJECTS + "/{id}/phases/{phaseId}", projectId, phaseId),
			ErrorCode.PROJECT_PHASE_IN_USE);

		final long otherProjectId = createProject(validCreateBody("PHASE-OTHER"));
		final long otherPhaseId = phasesOf(pm, otherProjectId).get(0).get("id").asLong();
		final Map<String, Object> crossProject = taskBody(null, "잘못된 단계", null,
			"2026-12-01", "2026-12-02", null);
		crossProject.put("phaseId", otherPhaseId);
		expectError(postJson(url(TASKS), pm, crossProject), ErrorCode.INVALID_WBS_PHASE);
	}

	@Test
	@DisplayName("3레벨 등록: 작업과 하위작업을 등록하면 새로고침(재조회) 후에도 순서·계층·기본 필드가 유지된다")
	void createTree() throws Exception {
		assertThat(tree(pm)).isEmpty();

		final long design = createTask(pm, null, "설계", developer.getId(), "2026-12-01", "2026-12-10");
		final long build = createTask(pm, null, "개발", null, "2026-12-11", "2027-01-31");
		final JsonNode change = readResponse(postJson(url(TASKS), pm, taskBody(design, "API 설계", qa.getId(),
			"2026-12-02", "2026-12-05", 2.5)));
		final long apiDesign = change.get("taskId").asLong();
		createTask(pm, design, "DB 설계", null, "2026-12-06", "2026-12-12");

		// 응답의 트리와 재조회한 트리가 같다
		assertThat(change.get("tasks").get(0).get("subtasks").get(0).get("id").asLong()).isEqualTo(apiDesign);
		final JsonNode tasks = tree(pm);
		assertThat(names(tasks)).containsExactly("설계", "개발");
		final JsonNode designNode = tasks.get(0);
		assertThat(designNode.get("id").asLong()).isEqualTo(design);
		assertThat(designNode.get("parentTaskId").isNull()).isTrue();
		assertThat(designNode.get("assignee").get("id").asLong()).isEqualTo(developer.getId());
		assertThat(designNode.get("assignee").get("name").asString()).isEqualTo(developer.getName());
		assertThat(names(designNode.get("subtasks"))).containsExactly("API 설계", "DB 설계");
		assertThat(designNode.get("subtasks").get(1).get("sortOrder").asInt()).isEqualTo(2);

		final JsonNode apiNode = designNode.get("subtasks").get(0);
		assertThat(apiNode.get("parentTaskId").asLong()).isEqualTo(design);
		assertThat(apiNode.get("estimatedEffort").decimalValue()).isEqualByComparingTo("2.5");
		assertThat(apiNode.get("status").asString()).isEqualTo("TODO");
		assertThat(apiNode.get("source").asString()).isEqualTo("MANUAL");
		assertThat(apiNode.get("progress").asInt()).isZero();
		assertThat(apiNode.get("scheduleDerived").asBoolean()).isFalse();
		assertThat(apiNode.get("subtasks")).isEmpty();

		// 상위 작업 기간은 하위작업 기간으로 산출(12-02 ~ 12-12)
		assertThat(designNode.get("scheduleDerived").asBoolean()).isTrue();
		assertThat(designNode.get("startDate").asString()).isEqualTo("2026-12-02");
		assertThat(designNode.get("endDate").asString()).isEqualTo("2026-12-12");
		assertThat(tasks.get(1).get("scheduleDerived").asBoolean()).isFalse();
		assertThat(tasks.get(1).get("assignee").isNull()).isTrue();
		assertThat(taskRepository.findById(build).orElseThrow().getEndDate()).isEqualTo("2027-01-31");
	}

	@Test
	@DisplayName("조회 권한: 멤버(개발자·현업 담당자)·공개 범위 열람자·소속 범위 경영진은 조회, 볼 수 없으면 404, WBS 기능을 끄면 404")
	void readPermissions() throws Exception {
		createTask(pm, null, "설계", null, "2026-12-01", "2026-12-10");

		for (final User viewer : List.of(developer, businessOwner, outsider, itExecutive)) {
			assertThat(names(tree(loginAs(viewer)))).containsExactly("설계");
		}

		final Map<String, Object> membersOnly = validCreateBody("MEM-2026");
		membersOnly.put("visibility", "MEMBERS_ONLY");
		final long membersOnlyId = createProject(membersOnly);
		expectError(getAs(loginAs(outsider), TASKS, membersOnlyId), ErrorCode.PROJECT_NOT_FOUND);
		expectError(getAs(loginAs(salesExecutive), TASKS, membersOnlyId), ErrorCode.PROJECT_NOT_FOUND);
		expectError(getAs(pm, TASKS, 999_999L), ErrorCode.PROJECT_NOT_FOUND);
		// 볼 수 없는 프로젝트에는 쓰기도 404(권한 403보다 먼저)
		expectError(postJson("/api/projects/" + membersOnlyId + "/wbs/tasks", loginAs(outsider),
			taskBody(null, "작업", null, "2026-12-01", "2026-12-02", null)), ErrorCode.PROJECT_NOT_FOUND);

		final Map<String, Object> noWbs = validCreateBody("NOWBS-2026");
		noWbs.put("features", List.of("KANBAN"));
		final long noWbsId = createProject(noWbs);
		expectError(getAs(pm, TASKS, noWbsId), ErrorCode.FEATURE_DISABLED);
		expectError(postJson("/api/projects/" + noWbsId + "/wbs/tasks", pm,
			taskBody(null, "작업", null, "2026-12-01", "2026-12-02", null)), ErrorCode.FEATURE_DISABLED);

		saveUser("admin01", USER_PASSWORD, SystemRole.SYSTEM_ADMIN, null);
		expectError(getAs(loginOk("admin01", USER_PASSWORD), TASKS, projectId), ErrorCode.NOT_FOUND);
	}

	@Test
	@DisplayName("권한 매트릭스: 개발자·QA는 담당 작업 아래 하위작업 추가·본인 작업 수정만, 현업 담당자·경영진·비멤버는 변경 불가")
	void writePermissions() throws Exception {
		final long devTask = createTask(pm, null, "개발자 작업", developer.getId(), "2026-12-01", "2026-12-10");
		final long qaTask = createTask(pm, null, "QA 작업", qa.getId(), "2026-12-01", "2026-12-10");
		final long pmTask = createTask(pm, null, "PM 작업", creator.getId(), "2026-12-01", "2026-12-10");
		final Cookie dev = loginAs(developer);
		final Cookie qaCookie = loginAs(qa);

		// 개발자: 담당 작업 아래 하위작업(담당자 비움·본인) 허용
		final long devSub = createTask(dev, devTask, "개발자 하위작업", developer.getId(), "2026-12-02", "2026-12-03");
		createTask(dev, devTask, "담당자 없는 하위작업", null, "2026-12-02", "2026-12-03");
		createTask(qaCookie, qaTask, "QA 하위작업", qa.getId(), "2026-12-02", "2026-12-03");
		// 최상위 작업 등록, 남의 작업 아래, 다른 사람에게 배정은 거부
		expectError(postJson(url(TASKS), dev, taskBody(null, "작업", null, "2026-12-01", "2026-12-02", null)),
			ErrorCode.PROJECT_PM_ONLY);
		expectError(postJson(url(TASKS), dev, taskBody(pmTask, "하위", null, "2026-12-01", "2026-12-02", null)),
			ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		expectError(postJson(url(TASKS), dev, taskBody(devTask, "하위", qa.getId(), "2026-12-01", "2026-12-02", null)),
			ErrorCode.PROJECT_PM_ONLY);

		// 개발자: 본인 작업 수정 허용, 남의 작업·담당자 변경·이동·삭제 거부
		final JsonNode devSubNode = node(devSub);
		readResponse(patchJson(dev, updateBody(devSubNode, "이름 변경", developer.getId(), "2026-12-02", "2026-12-04"),
			TASK, projectId, devSub));
		assertThat(node(devSub).get("name").asString()).isEqualTo("이름 변경");
		final JsonNode pmNode = node(pmTask);
		expectError(patchJson(dev, updateBody(pmNode, "변경", creator.getId(), "2026-12-01", "2026-12-10"),
			TASK, projectId, pmTask), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		expectError(patchJson(dev, updateBody(node(devSub), "이름 변경", qa.getId(), "2026-12-02", "2026-12-04"),
			TASK, projectId, devSub), ErrorCode.PROJECT_PM_ONLY);
		expectError(putJson(dev, moveBody(null, 0, node(devSub)), POSITION, projectId, devSub), ErrorCode.PROJECT_PM_ONLY);
		expectError(deleteAs(dev, TASK, projectId, devSub), ErrorCode.PROJECT_PM_ONLY);

		// 현업 담당자·경영진·멤버가 아닌 열람자: 조회만
		for (final User viewer : List.of(businessOwner, itExecutive, outsider)) {
			final Cookie cookie = loginAs(viewer);
			expectError(postJson(url(TASKS), cookie, taskBody(devTask, "하위", null, "2026-12-01", "2026-12-02", null)),
				ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
			expectError(patchJson(cookie, updateBody(pmNode, "변경", creator.getId(), "2026-12-01", "2026-12-10"),
				TASK, projectId, pmTask), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
			expectError(deleteAs(cookie, TASK, projectId, pmTask), ErrorCode.PROJECT_PM_ONLY);
		}
		assertThat(taskRepository.count()).isEqualTo(6);
	}

	@Test
	@DisplayName("3단계 중첩 거부: 하위작업 아래 등록·이동, 하위작업을 가진 작업을 다른 작업 아래로, 자기 자신 아래로는 400")
	void rejectThirdLevel() throws Exception {
		final long design = createTask(pm, null, "설계", null, "2026-12-01", "2026-12-10");
		final long build = createTask(pm, null, "개발", null, "2026-12-11", "2026-12-20");
		final long sub = createTask(pm, design, "하위", null, "2026-12-02", "2026-12-03");

		expectError(postJson(url(TASKS), pm, taskBody(sub, "3단계", null, "2026-12-02", "2026-12-03", null)),
			ErrorCode.INVALID_WBS_HIERARCHY);
		expectError(putJson(pm, moveBody(sub, 0, node(build)), POSITION, projectId, build), ErrorCode.INVALID_WBS_HIERARCHY);
		expectError(putJson(pm, moveBody(build, 0, node(design)), POSITION, projectId, design),
			ErrorCode.INVALID_WBS_HIERARCHY);
		expectError(putJson(pm, moveBody(build, 0, node(build)), POSITION, projectId, build),
			ErrorCode.INVALID_WBS_HIERARCHY);
		assertThat(names(tree(pm))).containsExactly("설계", "개발");
	}

	// ---- 이동 ----

	@Test
	@DisplayName("이동: 같은 부모 안 순서 변경·다른 작업 아래로·최상위로 승격 후 재조회해도 유지되고, 상위 기간이 다시 산출된다")
	void moveAndReload() throws Exception {
		final long a = createTask(pm, null, "A", null, "2026-12-01", "2026-12-10");
		final long b = createTask(pm, null, "B", null, "2026-12-11", "2026-12-20");
		final long c = createTask(pm, null, "C", null, "2026-12-21", "2026-12-31");
		final long a1 = createTask(pm, a, "A1", null, "2026-12-03", "2026-12-04");
		final long bVersionBefore = node(b).get("version").asLong();

		// 같은 부모 안: C를 맨 앞으로
		final JsonNode moved = readResponse(putJson(pm, moveBody(null, 0, node(c)), POSITION, projectId, c));
		assertThat(moved.get("taskId").asLong()).isEqualTo(c);
		assertThat(names(tree(pm))).containsExactly("C", "A", "B");
		assertThat(sortOrders(tree(pm))).containsExactly(1, 2, 3);
		// 형제 순서 변경은 형제의 version을 바꾸지 않는다(그 작업을 고치던 사용자가 409를 받지 않게)
		assertThat(node(b).get("version").asLong()).isEqualTo(bVersionBefore);

		// 작업 → 다른 작업의 하위작업: B를 A 아래 맨 뒤로, A 기간은 12-03 ~ 12-20
		readResponse(putJson(pm, moveBody(a, 1, node(b)), POSITION, projectId, b));
		JsonNode tasks = tree(pm);
		assertThat(names(tasks)).containsExactly("C", "A");
		assertThat(names(tasks.get(1).get("subtasks"))).containsExactly("A1", "B");
		assertThat(tasks.get(1).get("startDate").asString()).isEqualTo("2026-12-03");
		assertThat(tasks.get(1).get("endDate").asString()).isEqualTo("2026-12-20");

		// 하위작업 → 최상위 작업 승격: A1을 맨 앞으로, A 기간은 남은 B 기간(12-11 ~ 12-20)
		readResponse(putJson(pm, moveBody(null, 0, node(a1)), POSITION, projectId, a1));
		tasks = tree(pm);
		assertThat(names(tasks)).containsExactly("A1", "C", "A");
		assertThat(sortOrders(tasks)).containsExactly(1, 2, 3);
		assertThat(names(tasks.get(2).get("subtasks"))).containsExactly("B");
		assertThat(tasks.get(2).get("subtasks").get(0).get("sortOrder").asInt()).isEqualTo(1);
		assertThat(tasks.get(2).get("startDate").asString()).isEqualTo("2026-12-11");
		assertThat(tasks.get(2).get("endDate").asString()).isEqualTo("2026-12-20");

		// 마지막 하위작업을 옮기면 상위 작업은 마지막 산출 기간을 자기 기간으로 유지
		readResponse(putJson(pm, moveBody(null, 3, node(b)), POSITION, projectId, b));
		tasks = tree(pm);
		assertThat(names(tasks)).containsExactly("A1", "C", "A", "B");
		assertThat(tasks.get(2).get("scheduleDerived").asBoolean()).isFalse();
		assertThat(tasks.get(2).get("endDate").asString()).isEqualTo("2026-12-20");
	}

	@Test
	@DisplayName("이동 동시성·경계: 같은 위치 재전송은 version이 달라도 성공(멱등), 실제 이동에 낡은 version은 409, 범위 밖 위치는 400")
	void moveConcurrencyAndBoundaries() throws Exception {
		createTask(pm, null, "A", null, "2026-12-01", "2026-12-10");
		final long b = createTask(pm, null, "B", null, "2026-12-11", "2026-12-20");
		final JsonNode staleB = node(b);

		readResponse(putJson(pm, moveBody(null, 0, staleB), POSITION, projectId, b));
		// 같은 요청 재전송: 이미 그 위치라 그대로 성공
		readResponse(putJson(pm, moveBody(null, 0, staleB), POSITION, projectId, b));
		assertThat(names(tree(pm))).containsExactly("B", "A");

		// 다른 사용자가 B를 고친 뒤 낡은 version으로 이동
		readResponse(patchJson(pm, updateBody(node(b), "B2", null, "2026-12-11", "2026-12-20"), TASK, projectId, b));
		expectError(putJson(pm, moveBody(null, 1, staleB), POSITION, projectId, b), ErrorCode.CONCURRENT_MODIFICATION);

		// 위치 경계: 형제 수(1)는 맨 뒤로 허용, 2는 400, 음수는 검증 400
		readResponse(putJson(pm, moveBody(null, 1, node(b)), POSITION, projectId, b));
		assertThat(names(tree(pm))).containsExactly("A", "B2");
		expectError(putJson(pm, moveBody(null, 2, node(b)), POSITION, projectId, b), ErrorCode.INVALID_WBS_POSITION);
		putJson(pm, moveBody(null, -1, node(b)), POSITION, projectId, b).andExpect(status().isBadRequest());
		// 다른 프로젝트·없는 상위 작업은 404
		expectError(putJson(pm, moveBody(999_999L, 0, node(b)), POSITION, projectId, b), ErrorCode.WBS_TASK_NOT_FOUND);
	}

	// ---- 수정 ----

	@Test
	@DisplayName("수정: 낡은 version 409, 같은 값 재전송은 version이 달라도 성공, 하위작업 기간 변경은 상위 기간에 반영, 상위 기간 직접 변경은 400")
	void update() throws Exception {
		final long parent = createTask(pm, null, "설계", null, "2026-12-01", "2026-12-10");
		final long sub = createTask(pm, parent, "하위", null, "2026-12-02", "2026-12-03");
		final JsonNode stale = node(sub);

		readResponse(patchJson(pm, updateBody(stale, "하위", null, "2026-12-02", "2026-12-15"), TASK, projectId, sub));
		assertThat(node(parent).get("endDate").asString()).isEqualTo("2026-12-15");
		// 같은 값 재전송은 성공(이미 반영됨), 다른 값을 낡은 version으로 보내면 409
		readResponse(patchJson(pm, updateBody(stale, "하위", null, "2026-12-02", "2026-12-15"), TASK, projectId, sub));
		expectError(patchJson(pm, updateBody(stale, "다른 이름", null, "2026-12-02", "2026-12-15"), TASK, projectId, sub),
			ErrorCode.CONCURRENT_MODIFICATION);

		// 하위작업이 있는 작업: 기간을 바꾸면 400, 기간을 그대로 두고 다른 필드는 수정 가능
		final JsonNode parentNode = node(parent);
		expectError(patchJson(pm, updateBody(parentNode, "설계", null, "2026-12-01", "2026-12-15"), TASK, projectId, parent),
			ErrorCode.WBS_SCHEDULE_DERIVED);
		readResponse(patchJson(pm, updateBody(parentNode, "설계(수정)", developer.getId(), "2026-12-02", "2026-12-15"),
			TASK, projectId, parent));
		assertThat(node(parent).get("assignee").get("id").asLong()).isEqualTo(developer.getId());
	}

	@Test
	@DisplayName("값 검증: 기간 역전·프로젝트 기간 밖, 비멤버·비활성 담당자, 이름·공수 형식 오류는 400, 다른 프로젝트 작업은 404")
	void validation() throws Exception {
		expectError(postJson(url(TASKS), pm, taskBody(null, "작업", null, "2026-12-10", "2026-12-09", null)),
			ErrorCode.INVALID_WBS_SCHEDULE);
		expectError(postJson(url(TASKS), pm, taskBody(null, "작업", null, "2026-11-30", "2026-12-09", null)),
			ErrorCode.INVALID_WBS_SCHEDULE);
		expectError(postJson(url(TASKS), pm, taskBody(null, "작업", null, "2027-03-01", "2027-04-01", null)),
			ErrorCode.INVALID_WBS_SCHEDULE);
		expectError(postJson(url(TASKS), pm, taskBody(null, "작업", outsider.getId(), "2026-12-01", "2026-12-02", null)),
			ErrorCode.INVALID_TASK_ASSIGNEE);
		deactivate(qa);
		expectError(postJson(url(TASKS), pm, taskBody(null, "작업", qa.getId(), "2026-12-01", "2026-12-02", null)),
			ErrorCode.INVALID_TASK_ASSIGNEE);

		postJson(url(TASKS), pm, taskBody(null, " ", null, "2026-12-01", "2026-12-02", null))
			.andExpect(status().isBadRequest());
		postJson(url(TASKS), pm, taskBody(null, "가".repeat(201), null, "2026-12-01", "2026-12-02", null))
			.andExpect(status().isBadRequest());
		postJson(url(TASKS), pm, taskBody(null, "작업", null, "2026-12-01", "2026-12-02", 0.0))
			.andExpect(status().isBadRequest());
		postJson(url(TASKS), pm, taskBody(null, "작업", null, "2026-12-01", "2026-12-02", 3.55))
			.andExpect(status().isBadRequest());
		postJson(url(TASKS), pm, taskBody(null, "작업", null, "2026-12-01", null, null))
			.andExpect(status().isBadRequest());
		// 경계 통과: 이름 200자, 공수 0.1·99999.9, 프로젝트 첫날~마지막날
		createTask(pm, null, "가".repeat(200), null, "2026-12-01", "2027-03-31");
		readResponse(postJson(url(TASKS), pm, taskBody(null, "작업", null, "2026-12-01", "2026-12-01", 0.1)));
		readResponse(postJson(url(TASKS), pm, taskBody(null, "작업", null, "2026-12-01", "2026-12-01", 99999.9)));

		final long otherProject = createProject(validCreateBody("OTHER-2026"));
		final long otherTask = createTaskIn(otherProject, null, "다른 프로젝트 작업");
		expectError(patchJson(pm, updateBody(node(otherProject, otherTask), "변경", null, "2026-12-01", "2026-12-02"),
			TASK, projectId, otherTask), ErrorCode.WBS_TASK_NOT_FOUND);
		expectError(postJson(url(TASKS), pm, taskBody(otherTask, "하위", null, "2026-12-01", "2026-12-02", null)),
			ErrorCode.WBS_TASK_NOT_FOUND);
		expectError(deleteAs(pm, TASK, projectId, otherTask), ErrorCode.WBS_TASK_NOT_FOUND);
	}

	@Test
	@DisplayName("항목 수 상한: 프로젝트 WBS 항목이 2,000개면 더 등록할 수 없다(409)")
	void itemLimit() throws Exception {
		final List<Object[]> rows = new ArrayList<>();
		for (int i = 1; i <= WbsTaskService.MAX_ITEMS_PER_PROJECT - 1; i++) {
			rows.add(new Object[] {projectId, i, "작업" + i, creator.getId()});
		}
		jdbcTemplate.batchUpdate("""
			INSERT INTO wbs_tasks (project_id, sort_order, name, start_date, end_date, status, source, created_by,
			                       created_at, updated_at)
			VALUES (?, ?, ?, DATE '2026-12-01', DATE '2026-12-02', 'TODO', 'MANUAL', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
			""", rows);

		final long last = createTask(pm, null, "마지막", null, "2026-12-01", "2026-12-02");
		assertThat(taskRepository.findById(last).orElseThrow().getSortOrder()).isEqualTo(WbsTaskService.MAX_ITEMS_PER_PROJECT);
		expectError(postJson(url(TASKS), pm, taskBody(null, "초과", null, "2026-12-01", "2026-12-02", null)),
			ErrorCode.WBS_TASK_LIMIT_EXCEEDED);
	}

	// ---- 삭제 ----

	@Test
	@DisplayName("삭제: 하위작업·선후행·마일스톤 연결이 있으면 409, 하위작업을 지우면 상위 기간을 다시 산출하고 감사 로그를 남긴다")
	void delete() throws Exception {
		final long parent = createTask(pm, null, "설계", null, "2026-12-01", "2026-12-10");
		final long sub1 = createTask(pm, parent, "하위1", null, "2026-12-02", "2026-12-03");
		final long sub2 = createTask(pm, parent, "하위2", null, "2026-12-05", "2026-12-20");
		final long linked = createTask(pm, null, "선행", null, "2026-12-01", "2026-12-02");
		final long successor = createTask(pm, null, "후행", null, "2026-12-03", "2026-12-04");
		final long milestoneTask = createTask(pm, null, "마일스톤 작업", null, "2026-12-03", "2026-12-04");
		dependencyRepository.save(WbsDependency.finishToStart(projectId, linked, successor, creator.getId()));
		final Milestone milestone = milestoneRepository.save(Milestone.of(projectId, "설계 완료",
			LocalDate.of(2026, 12, 20), WbsItemSource.MANUAL, creator.getId()));
		milestoneTaskRepository.save(MilestoneTask.of(milestone.getId(), milestoneTask));

		expectError(deleteAs(pm, TASK, projectId, parent), ErrorCode.WBS_TASK_IN_USE);
		expectError(deleteAs(pm, TASK, projectId, linked), ErrorCode.WBS_TASK_IN_USE);
		expectError(deleteAs(pm, TASK, projectId, successor), ErrorCode.WBS_TASK_IN_USE);
		expectError(deleteAs(pm, TASK, projectId, milestoneTask), ErrorCode.WBS_TASK_IN_USE);

		final JsonNode deleted = readResponse(deleteAs(pm, TASK, projectId, sub2));
		assertThat(deleted.get("taskId").asLong()).isEqualTo(sub2);
		final JsonNode parentNode = node(parent);
		assertThat(names(parentNode.get("subtasks"))).containsExactly("하위1");
		assertThat(parentNode.get("startDate").asString()).isEqualTo("2026-12-02");
		assertThat(parentNode.get("endDate").asString()).isEqualTo("2026-12-03");
		assertThat(taskRepository.existsById(sub2)).isFalse();
		assertThat(taskRepository.existsById(sub1)).isTrue();
		// 다시 지우면 404(이미 없음)
		expectError(deleteAs(pm, TASK, projectId, sub2), ErrorCode.WBS_TASK_NOT_FOUND);

		final List<AuditLog> logs = auditLogs(AuditAction.WBS_TASK_DELETED);
		assertThat(logs).hasSize(1);
		assertThat(logs.get(0).getTargetId()).isEqualTo(sub2);
		assertThat(logs.get(0).getActorUserId()).isEqualTo(creator.getId());
		assertThat(logs.get(0).getDetails()).contains("\"parentTaskId\":" + parent, "\"name\":\"하위2\"");
	}

	// ---- 도우미 ----

	private String url(final String template) {
		return template.replace("{projectId}", String.valueOf(projectId));
	}

	private JsonNode tree(final Cookie cookie) throws Exception {
		return readResponse(getAs(cookie, TASKS, projectId)).get("tasks");
	}

	private long createTask(
		final Cookie cookie,
		final Long parentTaskId,
		final String name,
		final Long assigneeUserId,
		final String start,
		final String end
	) throws Exception {
		return readResponse(postJson(url(TASKS), cookie, taskBody(parentTaskId, name, assigneeUserId, start, end, null)))
			.get("taskId").asLong();
	}

	private long createTaskIn(final long otherProjectId, final Long parentTaskId, final String name) throws Exception {
		return readResponse(postJson("/api/projects/" + otherProjectId + "/wbs/tasks", pm,
			taskBody(parentTaskId, name, null, "2026-12-01", "2026-12-02", null))).get("taskId").asLong();
	}

	/** 트리에서 항목 하나(작업·하위작업)를 찾는다. */
	private JsonNode node(final long taskId) throws Exception {
		return node(projectId, taskId);
	}

	private JsonNode node(final long inProjectId, final long taskId) throws Exception {
		for (final JsonNode task : readResponse(getAs(pm, TASKS, inProjectId)).get("tasks")) {
			if (task.get("id").asLong() == taskId) {
				return task;
			}
			for (final JsonNode subtask : task.get("subtasks")) {
				if (subtask.get("id").asLong() == taskId) {
					return subtask;
				}
			}
		}
		throw new AssertionError("트리에 없는 작업: " + taskId);
	}

	private static Map<String, Object> taskBody(
		final Long parentTaskId,
		final String name,
		final Long assigneeUserId,
		final String start,
		final String end,
		final Double effort
	) {
		return body("parentTaskId", parentTaskId, "name", name, "description", null, "assigneeUserId", assigneeUserId,
			"startDate", start, "endDate", end, "estimatedEffort", effort);
	}

	private static Map<String, Object> updateBody(
		final JsonNode current,
		final String name,
		final Long assigneeUserId,
		final String start,
		final String end
	) {
		return body("name", name, "description", null, "assigneeUserId", assigneeUserId, "startDate", start,
			"endDate", end, "estimatedEffort", null, "version", current.get("version").asLong());
	}

	private static Map<String, Object> moveBody(final Long parentTaskId, final int index, final JsonNode current) {
		return body("parentTaskId", parentTaskId, "index", index, "version", current.get("version").asLong());
	}

	private static List<String> names(final JsonNode nodes) {
		return nodes.valueStream().map(node -> node.get("name").asString()).toList();
	}

	private static List<Integer> sortOrders(final JsonNode nodes) {
		return nodes.valueStream().map(node -> node.get("sortOrder").asInt()).toList();
	}
}
