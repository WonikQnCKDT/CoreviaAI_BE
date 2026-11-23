package org.wonik.coreviaai_be.domain.wbs.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
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
import org.springframework.test.web.servlet.ResultActions;
import org.wonik.coreviaai_be.domain.project.controller.ProjectApiTestSupport;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.service.BoardColumnService;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditLog;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.time.BusinessCalendar;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * FR-TSK-02 칸반 보드를 HTTP 경계에서 검증한다: 기본 컬럼, 보드 조회·담당자 필터, 카드 이동(상태 동기화·완료 처리·이력·잠금),
 * WIP 경고, 컬럼 관리 불변식, 권한(3.3 TSK-02), 기능 모듈, 마일스톤 지연 경고 해제(FR-WBS-04 연동).
 * <p>
 * 준비: 전사 공개 폭포수 프로젝트(WBS·칸반·산출물). 멤버 creator(PM)·developer(개발자)·qa(QA)·businessOwner(현업 담당자).
 * outsider는 멤버가 아닌 같은 부서 사용자, itExecutive는 소속 범위 경영진.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BoardApiIntegrationTest extends ProjectApiTestSupport {

	private static final String WBS_TASKS = PROJECTS + "/{projectId}/wbs/tasks";
	private static final String TASK = PROJECTS + "/{projectId}/tasks/{taskId}";
	private static final String HISTORIES = TASK + "/histories";
	private static final String BOARD = PROJECTS + "/{projectId}/board";
	private static final String CARD_COLUMN = BOARD + "/cards/{taskId}/column";
	private static final String COLUMNS = BOARD + "/columns";
	private static final String COLUMN = COLUMNS + "/{columnId}";
	private static final String COLUMN_ORDER = COLUMNS + "/order";
	private static final String MILESTONES = PROJECTS + "/{projectId}/wbs/milestones";

	@Autowired
	private WbsTaskRepository taskRepository;

	private User qa;
	private long projectId;
	private Cookie pm;
	private long todo;
	private long inProgress;
	private long review;
	private long done;

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
		final JsonNode columns = columns(pm);
		todo = columns.get(0).get("id").asLong();
		inProgress = columns.get(1).get("id").asLong();
		review = columns.get(2).get("id").asLong();
		done = columns.get(3).get("id").asLong();
	}

	// ---- 기본 컬럼 ----

	@Test
	@DisplayName("기본 컬럼: 프로젝트를 만들면 운영 방식과 관계없이 대기·진행·검토·완료 4개(기본 이름 = 상태 코드 라벨 키, WIP 없음)가 생긴다")
	void defaultColumns() throws Exception {
		final JsonNode columns = columns(pm);
		assertThat(columns).hasSize(4);
		assertThat(texts(columns, "statusCategory")).containsExactly("TODO", "IN_PROGRESS", "REVIEW", "DONE");
		assertThat(texts(columns, "labelKey")).containsExactly("code.TASK_STATUS.TODO", "code.TASK_STATUS.IN_PROGRESS",
			"code.TASK_STATUS.REVIEW", "code.TASK_STATUS.DONE");
		assertThat(columns.valueStream().map(column -> column.get("sortOrder").asInt()).toList())
			.containsExactly(1, 2, 3, 4);
		assertThat(columns).allSatisfy(column -> {
			assertThat(column.get("name").isNull()).isTrue();
			assertThat(column.get("wipLimit").isNull()).isTrue();
			assertThat(column.get("cardCount").asInt()).isZero();
			assertThat(column.get("wipExceeded").asBoolean()).isFalse();
			assertThat(column.get("cards")).isEmpty();
		});

		// 단계 없음·빈 템플릿·칸반 미사용 프로젝트에도 만든다(모듈을 나중에 켜도 바로 쓸 수 있게)
		final Map<String, Object> plain = validCreateBody("OPS-1");
		plain.put("template", "EMPTY");
		plain.put("methodology", "NONE");
		plain.put("features", List.of("WBS"));
		plain.put("gateApproverUserId", null);
		final long plainId = createProject(plain);
		assertThat(jdbcTemplate.queryForList("SELECT status_category FROM board_columns WHERE project_id = ?"
			+ " ORDER BY sort_order", String.class, plainId)).containsExactly("TODO", "IN_PROGRESS", "REVIEW", "DONE");
	}

	@Test
	@DisplayName("새 작업·하위작업은 대기 첫 컬럼에 놓이고 상태 변경 시각은 비어 있다")
	void newTaskPlacement() throws Exception {
		final long task = createTask("설계", null, developer.getId(), "2026-12-10");
		final long sub = createTask("API 설계", task, null, "2026-12-05");

		final WbsTask saved = taskRepository.findById(sub).orElseThrow();
		assertThat(saved.getBoardColumnId()).isEqualTo(todo);
		assertThat(saved.getStatusChangedAt()).isNull();
		assertThat(taskRepository.findById(task).orElseThrow().getBoardColumnId()).isEqualTo(todo);

		final JsonNode detail = readResponse(getAs(pm, TASK, projectId, sub));
		assertThat(detail.get("boardColumnId").asLong()).isEqualTo(todo);
		assertThat(detail.get("statusChangedAt").isNull()).isTrue();
	}

	// ---- 보드 조회 ----

	@Test
	@DisplayName("조회: 작업·하위작업이 모두 카드로 컬럼별(종료일 순) 보이고, 담당자 필터는 카드만 줄이며 카드 수·WIP는 전체 기준이다")
	void readBoardAndFilter() throws Exception {
		final List<Long> phaseIds = phasesOf(pm, projectId).valueStream()
			.map(phase -> phase.get("id").asLong()).toList();
		final long design = createTask("설계", null, developer.getId(), "2026-12-20");
		final long api = createTask("API 설계", design, qa.getId(), "2026-12-05");
		final Map<String, Object> buildRequest = taskBody("개발", null, null, "2026-12-31");
		buildRequest.put("phaseId", phaseIds.get(1));
		final long build = readResponse(postJson(url(WBS_TASKS), pm, buildRequest)).get("taskId").asLong();
		move(pm, build, inProgress);

		final JsonNode columns = columns(pm);
		final JsonNode todoColumn = columns.get(0);
		// 상위 작업 기간은 하위작업 기간으로 산출되어(12-05) 종료일이 같으므로 ID 순
		assertThat(ids(todoColumn.get("cards"))).containsExactly(design, api);
		assertThat(todoColumn.get("cardCount").asInt()).isEqualTo(2);
		final JsonNode apiCard = todoColumn.get("cards").get(1);
		assertThat(apiCard.get("parentTaskId").asLong()).isEqualTo(design);
		assertThat(apiCard.get("parentTaskName").asString()).isEqualTo("설계");
		assertThat(apiCard.get("assignee").get("name").asString()).isEqualTo(qa.getName());
		assertThat(apiCard.get("status").asString()).isEqualTo("TODO");
		assertThat(apiCard.get("taskType").asString()).isEqualTo("TASK");
		assertThat(apiCard.get("priority").asString()).isEqualTo("MEDIUM");
		assertThat(apiCard.get("version").asLong()).isZero();
		assertThat(todoColumn.get("cards").get(0).get("parentTaskName").isNull()).isTrue();
		final JsonNode buildCard = columns.get(1).get("cards").get(0);
		assertThat(buildCard.get("id").asLong()).isEqualTo(build);
		assertThat(buildCard.get("phaseId").asLong()).isEqualTo(phaseIds.get(1));
		assertThat(buildCard.get("assignee").isNull()).isTrue();

		final JsonNode filtered = readResponse(getAs(pm, BOARD + "?assigneeUserId=" + developer.getId(), projectId))
			.get("columns");
		assertThat(ids(filtered.get(0).get("cards"))).containsExactly(design);
		assertThat(filtered.get(0).get("cardCount").asInt()).isEqualTo(2);
		assertThat(filtered.get(1).get("cards")).isEmpty();
		assertThat(filtered.get(1).get("cardCount").asInt()).isEqualTo(1);
		final JsonNode phaseFiltered = readResponse(getAs(pm, BOARD + "?phaseId=" + phaseIds.get(1), projectId))
			.get("columns");
		assertThat(phaseFiltered.get(0).get("cards")).isEmpty();
		assertThat(ids(phaseFiltered.get(1).get("cards"))).containsExactly(build);

		final long otherProjectId = createProject(validCreateBody("BOARD-PHASE"));
		final long otherPhaseId = phasesOf(pm, otherProjectId).get(0).get("id").asLong();
		expectError(getAs(pm, BOARD + "?phaseId=" + otherPhaseId, projectId), ErrorCode.PROJECT_PHASE_NOT_FOUND);

		getAs(pm, BOARD + "?assigneeUserId=0", projectId).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("조회 권한: 멤버·공개 범위 열람자·소속 범위 경영진은 조회, 볼 수 없으면 404, 칸반 꺼짐 404, 관리자 404")
	void readPermissions() throws Exception {
		createTask("설계", null, developer.getId(), "2026-12-10");
		for (final User viewer : List.of(developer, qa, businessOwner, outsider, itExecutive)) {
			assertThat(columns(loginAs(viewer)).get(0).get("cardCount").asInt()).isEqualTo(1);
		}

		final Map<String, Object> membersOnly = validCreateBody("MEM-2026");
		membersOnly.put("visibility", "MEMBERS_ONLY");
		final long membersOnlyId = createProject(membersOnly);
		expectError(getAs(loginAs(outsider), BOARD, membersOnlyId), ErrorCode.PROJECT_NOT_FOUND);
		expectError(getAs(loginAs(salesExecutive), BOARD, membersOnlyId), ErrorCode.PROJECT_NOT_FOUND);
		expectError(getAs(pm, BOARD, 999_999L), ErrorCode.PROJECT_NOT_FOUND);

		final Map<String, Object> noKanban = validCreateBody("NOKB-2026");
		noKanban.put("features", List.of("WBS"));
		final long noKanbanId = createProject(noKanban);
		expectError(getAs(pm, BOARD, noKanbanId), ErrorCode.FEATURE_DISABLED);
		expectError(postJson(COLUMNS.replace("{projectId}", String.valueOf(noKanbanId)), pm,
			columnBody("리뷰", "REVIEW", null)), ErrorCode.FEATURE_DISABLED);

		saveUser("admin01", USER_PASSWORD, SystemRole.SYSTEM_ADMIN, null);
		expectError(getAs(loginOk("admin01", USER_PASSWORD), BOARD, projectId), ErrorCode.NOT_FOUND);
	}

	@Test
	@DisplayName("칸반만 켠 프로젝트: 보드·업무 상세·카드 이동은 되고 WBS 트리는 404. 컬럼이 비어 있는 기존 작업은 같은 상태 첫 컬럼에 보인다")
	void kanbanOnlyProject() throws Exception {
		final Map<String, Object> kanbanOnly = validCreateBody("KB-2026");
		kanbanOnly.put("features", List.of("KANBAN"));
		final long kanbanId = createProject(kanbanOnly);
		jdbcTemplate.update("""
			INSERT INTO wbs_tasks (project_id, sort_order, name, start_date, end_date, status, source, created_by,
			                       created_at, updated_at)
			VALUES (?, 1, '이관 작업', DATE '2026-12-01', DATE '2026-12-02', 'TODO', 'MANUAL', ?, CURRENT_TIMESTAMP,
			        CURRENT_TIMESTAMP)
			""", kanbanId, creator.getId());
		final long taskId = jdbcTemplate.queryForObject("SELECT id FROM wbs_tasks WHERE project_id = ?", Long.class,
			kanbanId);

		final JsonNode columns = readResponse(getAs(pm, BOARD, kanbanId)).get("columns");
		assertThat(ids(columns.get(0).get("cards"))).containsExactly(taskId);
		assertThat(readResponse(getAs(pm, TASK, kanbanId, taskId)).get("name").asString()).isEqualTo("이관 작업");
		expectError(getAs(pm, WBS_TASKS, kanbanId), ErrorCode.FEATURE_DISABLED);

		final JsonNode moved = readResponse(putJson(pm, moveBody(columns.get(1).get("id").asLong(), 0L), CARD_COLUMN,
			kanbanId, taskId));
		assertThat(moved.get("card").get("status").asString()).isEqualTo("IN_PROGRESS");
	}

	// ---- 카드 이동 ----

	@Test
	@DisplayName("이동: 상태가 컬럼 상태로 바뀌고 상태 변경 시각을 남기며, 완료로 옮기면 완료 시각·진척률 100, 완료에서 벗어나면 완료 시각만 지운다")
	void moveSyncsStatus() throws Exception {
		final long taskId = createTask("설계", null, developer.getId(), "2026-12-10");
		final Instant started = clock.instant();

		final JsonNode first = readResponse(putJson(pm, moveBody(inProgress, 0L), CARD_COLUMN, projectId, taskId));
		assertThat(first.get("card").get("status").asString()).isEqualTo("IN_PROGRESS");
		assertThat(Instant.parse(first.get("card").get("statusChangedAt").asString())).isEqualTo(started);
		assertThat(first.get("card").get("completedAt").isNull()).isTrue();
		assertThat(first.get("card").get("version").asLong()).isEqualTo(1);
		assertThat(first.get("column").get("id").asLong()).isEqualTo(inProgress);
		assertThat(first.get("column").get("cardCount").asInt()).isEqualTo(1);
		assertThat(first.get("wipWarning").asBoolean()).isFalse();

		clock.advance(Duration.ofMinutes(5));
		final Instant finished = clock.instant();
		final JsonNode completed = readResponse(putJson(pm, moveBody(done, 1L), CARD_COLUMN, projectId, taskId));
		assertThat(completed.get("card").get("status").asString()).isEqualTo("DONE");
		assertThat(completed.get("card").get("progress").asInt()).isEqualTo(100);
		assertThat(Instant.parse(completed.get("card").get("completedAt").asString())).isEqualTo(finished);
		final JsonNode detail = readResponse(getAs(pm, TASK, projectId, taskId));
		assertThat(detail.get("status").asString()).isEqualTo("DONE");
		assertThat(detail.get("boardColumnId").asLong()).isEqualTo(done);
		assertThat(Instant.parse(detail.get("completedAt").asString())).isEqualTo(finished);
		assertThat(Instant.parse(detail.get("statusChangedAt").asString())).isEqualTo(finished);

		clock.advance(Duration.ofMinutes(5));
		final JsonNode reopened = readResponse(putJson(pm, moveBody(review, 2L), CARD_COLUMN, projectId, taskId));
		assertThat(reopened.get("card").get("status").asString()).isEqualTo("REVIEW");
		assertThat(reopened.get("card").get("completedAt").isNull()).isTrue();
		assertThat(reopened.get("card").get("progress").asInt()).isEqualTo(100);

		final JsonNode items = readResponse(getAs(pm, HISTORIES + "?size=100", projectId, taskId)).get("items");
		assertThat(texts(items, "activityType")).containsExactly("CREATED", "STATUS_CHANGED", "STATUS_CHANGED",
			"PROGRESS_CHANGED", "STATUS_CHANGED");
		final JsonNode toDone = items.get(2);
		assertThat(toDone.get("before").get("status").asString()).isEqualTo("IN_PROGRESS");
		assertThat(toDone.get("before").get("boardColumnId").asLong()).isEqualTo(inProgress);
		assertThat(toDone.get("after").get("status").asString()).isEqualTo("DONE");
		assertThat(toDone.get("after").get("boardColumnId").asLong()).isEqualTo(done);
		assertThat(toDone.get("actor").get("id").asLong()).isEqualTo(creator.getId());
		assertThat(items.get(3).get("after").get("progress").asInt()).isEqualTo(100);
	}

	@Test
	@DisplayName("이동 멱등·잠금: 같은 컬럼 재전송은 version과 무관하게 그대로 성공(이력 없음), 다른 컬럼은 옛 version이면 409")
	void moveIdempotentAndOptimisticLock() throws Exception {
		final long taskId = createTask("설계", null, null, "2026-12-10");
		readResponse(putJson(pm, moveBody(inProgress, 0L), CARD_COLUMN, projectId, taskId));

		final JsonNode again = readResponse(putJson(pm, moveBody(inProgress, 0L), CARD_COLUMN, projectId, taskId));
		assertThat(again.get("card").get("version").asLong()).isEqualTo(1);
		expectError(putJson(pm, moveBody(review, 0L), CARD_COLUMN, projectId, taskId),
			ErrorCode.CONCURRENT_MODIFICATION);
		assertThat(taskRepository.findById(taskId).orElseThrow().getBoardColumnId()).isEqualTo(inProgress);

		// WBS 수정과 version을 공유한다
		patchJson(pm, body("name", "설계(수정)", "description", null, "assigneeUserId", null, "startDate", "2026-12-01",
			"endDate", "2026-12-10", "estimatedEffort", null, "version", 1L), WBS_TASKS + "/{taskId}", projectId, taskId)
			.andExpect(status().isOk());
		expectError(putJson(pm, moveBody(review, 1L), CARD_COLUMN, projectId, taskId),
			ErrorCode.CONCURRENT_MODIFICATION);
		assertThat(readResponse(putJson(pm, moveBody(review, 2L), CARD_COLUMN, projectId, taskId))
			.get("card").get("status").asString()).isEqualTo("REVIEW");

		final JsonNode items = readResponse(getAs(pm, HISTORIES + "?size=100", projectId, taskId)).get("items");
		assertThat(texts(items, "activityType")).containsExactly("CREATED", "STATUS_CHANGED", "DETAILS_CHANGED",
			"STATUS_CHANGED");
	}

	@Test
	@DisplayName("이동 권한: PM은 전체, 개발자·QA는 본인 카드만, 현업 담당자(담당이어도)·비멤버·경영진은 403, 볼 수 없으면 404")
	void movePermissions() throws Exception {
		final long devTask = createTask("개발", null, developer.getId(), "2026-12-10");
		final long qaTask = createTask("테스트", null, qa.getId(), "2026-12-10");
		final long ownerTask = createTask("요구사항 확인", null, businessOwner.getId(), "2026-12-10");
		final Cookie dev = loginAs(developer);
		final Cookie qaCookie = loginAs(qa);

		assertThat(readResponse(putJson(dev, moveBody(inProgress, 0L), CARD_COLUMN, projectId, devTask))
			.get("card").get("status").asString()).isEqualTo("IN_PROGRESS");
		assertThat(readResponse(putJson(qaCookie, moveBody(review, 0L), CARD_COLUMN, projectId, qaTask))
			.get("card").get("status").asString()).isEqualTo("REVIEW");
		expectError(putJson(dev, moveBody(done, 1L), CARD_COLUMN, projectId, qaTask), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		expectError(putJson(qaCookie, moveBody(done, 1L), CARD_COLUMN, projectId, devTask),
			ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		// 담당 403이 컬럼 404보다 먼저
		expectError(putJson(dev, moveBody(999_999L, 1L), CARD_COLUMN, projectId, qaTask),
			ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		for (final User reader : List.of(businessOwner, outsider, itExecutive)) {
			expectError(putJson(loginAs(reader), moveBody(inProgress, 0L), CARD_COLUMN, projectId, ownerTask),
				ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		}
		assertThat(readResponse(putJson(pm, moveBody(done, 1L), CARD_COLUMN, projectId, qaTask))
			.get("card").get("status").asString()).isEqualTo("DONE");

		final Map<String, Object> membersOnly = validCreateBody("MEM-2026");
		membersOnly.put("visibility", "MEMBERS_ONLY");
		final long membersOnlyId = createProject(membersOnly);
		final long hiddenTask = readResponse(postJson(WBS_TASKS.replace("{projectId}", String.valueOf(membersOnlyId)),
			pm, taskBody("비공개", null, null, "2026-12-10"))).get("taskId").asLong();
		final long otherColumn = readResponse(getAs(pm, BOARD, membersOnlyId)).get("columns").get(1).get("id").asLong();
		expectError(putJson(loginAs(outsider), moveBody(otherColumn, 0L), CARD_COLUMN, membersOnlyId, hiddenTask),
			ErrorCode.PROJECT_NOT_FOUND);
		// 다른 프로젝트의 업무·컬럼 ID(IDOR)는 404
		expectError(putJson(pm, moveBody(inProgress, 0L), CARD_COLUMN, projectId, hiddenTask),
			ErrorCode.WBS_TASK_NOT_FOUND);
		expectError(putJson(pm, moveBody(otherColumn, 0L), CARD_COLUMN, projectId, ownerTask),
			ErrorCode.BOARD_COLUMN_NOT_FOUND);
		expectError(putJson(pm, moveBody(999_999L, 0L), CARD_COLUMN, projectId, ownerTask),
			ErrorCode.BOARD_COLUMN_NOT_FOUND);

		putJson(pm, body("columnId", null, "version", 0L), CARD_COLUMN, projectId, ownerTask)
			.andExpect(status().isBadRequest());
		putJson(pm, body("columnId", inProgress, "version", -1L), CARD_COLUMN, projectId, ownerTask)
			.andExpect(status().isBadRequest());
		putJson(pm, body("columnId", inProgress), CARD_COLUMN, projectId, ownerTask)
			.andExpect(status().isBadRequest());
	}

	// ---- WIP ----

	@Test
	@DisplayName("WIP: 한도와 같으면 경고 없음, 넘으면 이동은 반영되고 wipWarning·wipExceeded가 true, 한도를 올리거나 없애면 해제")
	void wipWarning() throws Exception {
		final long first = createTask("A", null, null, "2026-12-10");
		final long second = createTask("B", null, null, "2026-12-11");
		patchJson(pm, body("name", null, "wipLimit", 1), COLUMN, projectId, inProgress).andExpect(status().isOk());

		final JsonNode atLimit = readResponse(putJson(pm, moveBody(inProgress, 0L), CARD_COLUMN, projectId, first));
		assertThat(atLimit.get("wipWarning").asBoolean()).isFalse();
		assertThat(atLimit.get("column").get("wipLimit").asInt()).isEqualTo(1);

		final JsonNode over = readResponse(putJson(pm, moveBody(inProgress, 0L), CARD_COLUMN, projectId, second));
		assertThat(over.get("wipWarning").asBoolean()).isTrue();
		assertThat(over.get("column").get("cardCount").asInt()).isEqualTo(2);
		assertThat(over.get("column").get("wipExceeded").asBoolean()).isTrue();
		assertThat(over.get("card").get("status").asString()).isEqualTo("IN_PROGRESS");
		assertThat(columns(pm).get(1).get("wipExceeded").asBoolean()).isTrue();

		// 담당자 필터로 카드가 줄어도 경고는 컬럼 전체 기준
		final JsonNode filtered = readResponse(getAs(pm, BOARD + "?assigneeUserId=" + developer.getId(), projectId))
			.get("columns");
		assertThat(filtered.get(1).get("wipExceeded").asBoolean()).isTrue();

		final JsonNode raised = readResponse(patchJson(pm, body("name", null, "wipLimit", 2), COLUMN, projectId,
			inProgress)).get("columns");
		assertThat(raised.get(1).get("wipExceeded").asBoolean()).isFalse();
		final JsonNode removed = readResponse(patchJson(pm, body("name", null, "wipLimit", null), COLUMN, projectId,
			inProgress)).get("columns");
		assertThat(removed.get(1).get("wipLimit").isNull()).isTrue();
	}

	@Test
	@DisplayName("카드 수(COUNT): 컬럼이 비어 있는 카드는 같은 상태 첫 컬럼의 카드 수에만 들어가 WIP 경고·삭제 판정에 반영된다")
	void cardCountIncludesUnplacedCardsOnlyInFirstColumn() throws Exception {
		final long placed = createTask("A", null, null, "2026-12-10");
		final long unplaced = createTask("B", null, null, "2026-12-11");
		jdbcTemplate.update("UPDATE wbs_tasks SET board_column_id = NULL WHERE id = ?", unplaced);
		final long backlog = lastColumnId(readResponse(postJson(url(COLUMNS), pm, columnBody("백로그", "TODO", 1))));
		patchJson(pm, body("name", null, "wipLimit", 1), COLUMN, projectId, todo).andExpect(status().isOk());

		// 첫 대기 컬럼이 아닌 백로그에는 컬럼이 비어 있는 카드가 들어가지 않는다(한도와 같음 → 경고 없음)
		final JsonNode toBacklog = readResponse(move(pm, placed, backlog));
		assertThat(toBacklog.get("column").get("cardCount").asInt()).isEqualTo(1);
		assertThat(toBacklog.get("wipWarning").asBoolean()).isFalse();

		// 첫 대기 컬럼 = 놓인 카드 + 컬럼이 비어 있는 대기 카드(보드 조회와 같은 수)
		final JsonNode toTodo = readResponse(move(pm, placed, todo));
		assertThat(toTodo.get("column").get("cardCount").asInt()).isEqualTo(2);
		assertThat(toTodo.get("wipWarning").asBoolean()).isTrue();
		assertThat(columns(pm).get(0).get("cardCount").asInt()).isEqualTo(2);

		// 컬럼이 비어 있는 카드만 남아도 첫 대기 컬럼은 비지 않은 것이라 삭제 409
		move(pm, placed, backlog);
		expectError(deleteAs(pm, COLUMN, projectId, todo), ErrorCode.BOARD_COLUMN_NOT_EMPTY);
	}

	// ---- 컬럼 관리 ----

	@Test
	@DisplayName("컬럼 추가·변경: 맨 뒤에 붙고, 이름을 비우면 기본 이름(라벨 키)으로, 같은 값 재전송은 감사 로그 없음. 입력 경계값은 400")
	void createAndUpdateColumns() throws Exception {
		final JsonNode created = readResponse(postJson(url(COLUMNS), pm, columnBody("  코드 리뷰  ", "REVIEW", 3)))
			.get("columns");
		assertThat(created).hasSize(5);
		final JsonNode codeReview = created.get(4);
		assertThat(codeReview.get("name").asString()).isEqualTo("코드 리뷰");
		assertThat(codeReview.get("labelKey").isNull()).isTrue();
		assertThat(codeReview.get("statusCategory").asString()).isEqualTo("REVIEW");
		assertThat(codeReview.get("sortOrder").asInt()).isEqualTo(5);
		assertThat(codeReview.get("wipLimit").asInt()).isEqualTo(3);
		final long codeReviewId = codeReview.get("id").asLong();
		assertThat(auditLogs(AuditAction.BOARD_COLUMN_CREATED)).singleElement().satisfies(log -> {
			assertThat(log.getTargetType()).isEqualTo(AuditTargetType.BOARD_COLUMN);
			assertThat(log.getTargetId()).isEqualTo(codeReviewId);
		});

		final JsonNode blankName = readResponse(postJson(url(COLUMNS), pm, columnBody("   ", "DONE", null)))
			.get("columns").get(5);
		assertThat(blankName.get("name").isNull()).isTrue();
		assertThat(blankName.get("labelKey").asString()).isEqualTo("code.TASK_STATUS.DONE");

		final JsonNode renamed = readResponse(patchJson(pm, body("name", "백로그", "wipLimit", null), COLUMN, projectId,
			todo)).get("columns").get(0);
		assertThat(renamed.get("name").asString()).isEqualTo("백로그");
		assertThat(renamed.get("labelKey").isNull()).isTrue();
		patchJson(pm, body("name", "백로그", "wipLimit", null), COLUMN, projectId, todo).andExpect(status().isOk());
		assertThat(auditLogs(AuditAction.BOARD_COLUMN_CHANGED)).singleElement().extracting(AuditLog::getDetails)
			.asString().contains("\"name\":{\"before\":null,\"after\":\"백로그\"}");
		final JsonNode reverted = readResponse(patchJson(pm, body("name", null, "wipLimit", null), COLUMN, projectId,
			todo)).get("columns").get(0);
		assertThat(reverted.get("name").isNull()).isTrue();
		assertThat(reverted.get("labelKey").asString()).isEqualTo("code.TASK_STATUS.TODO");

		final String fiftyOne = "가".repeat(51);
		postJson(url(COLUMNS), pm, columnBody(fiftyOne, "TODO", null)).andExpect(status().isBadRequest());
		assertThat(readResponse(postJson(url(COLUMNS), pm, columnBody("가".repeat(50), "TODO", 999))).get("columns"))
			.hasSize(7);
		postJson(url(COLUMNS), pm, columnBody("A", "TODO", 0)).andExpect(status().isBadRequest());
		postJson(url(COLUMNS), pm, columnBody("A", "TODO", 1000)).andExpect(status().isBadRequest());
		postJson(url(COLUMNS), pm, columnBody("A", null, null)).andExpect(status().isBadRequest());
		postJson(url(COLUMNS), pm, columnBody("A", "BLOCKED", null)).andExpect(status().isBadRequest());
		patchJson(pm, body("name", fiftyOne, "wipLimit", null), COLUMN, projectId, todo)
			.andExpect(status().isBadRequest());
		patchJson(pm, body("name", null, "wipLimit", -1), COLUMN, projectId, todo).andExpect(status().isBadRequest());
		// 컬럼 이름은 한 줄 문구: 줄바꿈·탭 등 제어 문자는 400
		postJson(url(COLUMNS), pm, columnBody("코드\n리뷰", "TODO", null)).andExpect(status().isBadRequest());
		patchJson(pm, body("name", "탭\t이름", "wipLimit", null), COLUMN, projectId, todo)
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("컬럼 상한: 프로젝트당 20개까지, 21번째는 409")
	void columnLimit() throws Exception {
		for (int i = 5; i <= BoardColumnService.MAX_COLUMNS_PER_PROJECT; i++) {
			postJson(url(COLUMNS), pm, columnBody("컬럼" + i, "IN_PROGRESS", null)).andExpect(status().isOk());
		}
		expectError(postJson(url(COLUMNS), pm, columnBody("넘침", "IN_PROGRESS", null)),
			ErrorCode.BOARD_COLUMN_LIMIT_EXCEEDED);
		assertThat(columns(pm)).hasSize(BoardColumnService.MAX_COLUMNS_PER_PROJECT);
	}

	@Test
	@DisplayName("컬럼 순서: 모든 컬럼을 한 번씩 보내면 1부터 다시 매기고, 빠짐·중복·다른 프로젝트 컬럼은 400")
	void reorderColumns() throws Exception {
		final JsonNode reordered = readResponse(putJson(pm, body("columnIds", List.of(done, review, inProgress, todo)),
			COLUMN_ORDER, projectId)).get("columns");
		assertThat(ids(reordered)).containsExactly(done, review, inProgress, todo);
		assertThat(reordered.valueStream().map(column -> column.get("sortOrder").asInt()).toList())
			.containsExactly(1, 2, 3, 4);
		assertThat(ids(columns(pm))).containsExactly(done, review, inProgress, todo);
		assertThat(auditLogs(AuditAction.BOARD_COLUMNS_REORDERED)).singleElement().satisfies(log -> {
			assertThat(log.getTargetType()).isEqualTo(AuditTargetType.PROJECT);
			assertThat(log.getTargetId()).isEqualTo(projectId);
		});
		// 같은 순서 재전송은 아무것도 바꾸지 않음
		putJson(pm, body("columnIds", List.of(done, review, inProgress, todo)), COLUMN_ORDER, projectId)
			.andExpect(status().isOk());
		assertThat(auditLogs(AuditAction.BOARD_COLUMNS_REORDERED)).hasSize(1);

		expectError(putJson(pm, body("columnIds", List.of(done, review, inProgress)), COLUMN_ORDER, projectId),
			ErrorCode.INVALID_BOARD_COLUMN_ORDER);
		expectError(putJson(pm, body("columnIds", List.of(done, review, inProgress, inProgress)), COLUMN_ORDER,
			projectId), ErrorCode.INVALID_BOARD_COLUMN_ORDER);
		final long otherProject = createProject(validCreateBody("OTH-2026"));
		final long foreign = readResponse(getAs(pm, BOARD, otherProject)).get("columns").get(0).get("id").asLong();
		expectError(putJson(pm, body("columnIds", List.of(done, review, inProgress, foreign)), COLUMN_ORDER,
			projectId), ErrorCode.INVALID_BOARD_COLUMN_ORDER);
		putJson(pm, body("columnIds", List.of()), COLUMN_ORDER, projectId).andExpect(status().isBadRequest());
		final List<Long> nullId = new ArrayList<>(List.of(done, review, inProgress));
		nullId.add(null);
		putJson(pm, body("columnIds", nullId), COLUMN_ORDER, projectId).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("컬럼 삭제: 상태별 마지막 컬럼·카드가 있는 컬럼은 409, 비면 삭제(감사 로그). 대기 첫 컬럼을 지우면 새 작업은 남은 대기 컬럼에 놓인다")
	void deleteColumns() throws Exception {
		expectError(deleteAs(pm, COLUMN, projectId, todo), ErrorCode.BOARD_COLUMN_LAST_OF_STATUS);
		expectError(deleteAs(pm, COLUMN, projectId, done), ErrorCode.BOARD_COLUMN_LAST_OF_STATUS);

		final long backlog = lastColumnId(readResponse(postJson(url(COLUMNS), pm, columnBody("백로그", "TODO", null))));
		final long taskId = createTask("설계", null, null, "2026-12-10");
		assertThat(taskRepository.findById(taskId).orElseThrow().getBoardColumnId()).isEqualTo(todo);
		expectError(deleteAs(pm, COLUMN, projectId, todo), ErrorCode.BOARD_COLUMN_NOT_EMPTY);

		// 같은 상태의 다른 컬럼으로 옮기면 상태는 그대로, BOARD_COLUMN_CHANGED 이력
		final JsonNode moved = readResponse(putJson(pm, moveBody(backlog, 0L), CARD_COLUMN, projectId, taskId));
		assertThat(moved.get("card").get("status").asString()).isEqualTo("TODO");
		assertThat(moved.get("card").get("statusChangedAt").isNull()).isTrue();
		final JsonNode items = readResponse(getAs(pm, HISTORIES + "?size=100", projectId, taskId)).get("items");
		assertThat(texts(items, "activityType")).containsExactly("CREATED", "BOARD_COLUMN_CHANGED");

		final JsonNode remaining = readResponse(deleteAs(pm, COLUMN, projectId, todo)).get("columns");
		assertThat(ids(remaining)).containsExactly(inProgress, review, done, backlog);
		assertThat(auditLogs(AuditAction.BOARD_COLUMN_DELETED)).singleElement().satisfies(log -> {
			assertThat(log.getTargetType()).isEqualTo(AuditTargetType.BOARD_COLUMN);
			assertThat(log.getTargetId()).isEqualTo(todo);
		});
		expectError(deleteAs(pm, COLUMN, projectId, todo), ErrorCode.BOARD_COLUMN_NOT_FOUND);
		expectError(deleteAs(pm, COLUMN, projectId, backlog), ErrorCode.BOARD_COLUMN_LAST_OF_STATUS);

		final long next = createTask("개발", null, null, "2026-12-20");
		assertThat(taskRepository.findById(next).orElseThrow().getBoardColumnId()).isEqualTo(backlog);
	}

	@Test
	@DisplayName("컬럼 관리 권한: PM만. 개발자·QA·현업 담당자·경영진은 403, 다른 프로젝트 컬럼은 404")
	void columnPermissions() throws Exception {
		for (final User user : List.of(developer, qa, businessOwner, itExecutive)) {
			final Cookie cookie = loginAs(user);
			expectError(postJson(url(COLUMNS), cookie, columnBody("리뷰", "REVIEW", null)), ErrorCode.PROJECT_PM_ONLY);
			expectError(patchJson(cookie, body("name", "리뷰", "wipLimit", 3), COLUMN, projectId, review),
				ErrorCode.PROJECT_PM_ONLY);
			expectError(putJson(cookie, body("columnIds", List.of(done, review, inProgress, todo)), COLUMN_ORDER,
				projectId), ErrorCode.PROJECT_PM_ONLY);
			expectError(deleteAs(cookie, COLUMN, projectId, review), ErrorCode.PROJECT_PM_ONLY);
		}
		assertThat(columns(pm)).hasSize(4);

		final long otherProject = createProject(validCreateBody("OTH-2026"));
		final long foreign = readResponse(getAs(pm, BOARD, otherProject)).get("columns").get(2).get("id").asLong();
		expectError(patchJson(pm, body("name", "리뷰", "wipLimit", null), COLUMN, projectId, foreign),
			ErrorCode.BOARD_COLUMN_NOT_FOUND);
		expectError(deleteAs(pm, COLUMN, projectId, foreign), ErrorCode.BOARD_COLUMN_NOT_FOUND);
	}

	// ---- 마일스톤 지연 경고 연동(SCRUM-108 미결 6) ----

	@Test
	@DisplayName("마일스톤: 연결 작업을 보드에서 완료로 옮기면 지연 경고가 해제되고, 다시 검토로 옮기면 경고가 돌아온다")
	void milestoneDelayFollowsBoard() throws Exception {
		final long taskId = createTask("오픈 준비", null, null, "2026-12-10");
		final long milestoneId = readResponse(postJson(url(MILESTONES), pm, body("name", "오픈", "targetDate",
			"2026-12-10", "taskIds", List.of(taskId)))).get("id").asLong();
		clock.setInstant(LocalDateTime.of(2026, 12, 10, 9, 0).atZone(BusinessCalendar.ZONE).toInstant());
		pm = loginAs(creator);
		assertThat(milestone(milestoneId).get("delayed").asBoolean()).isTrue();

		putJson(pm, moveBody(done, 0L), CARD_COLUMN, projectId, taskId).andExpect(status().isOk());
		final JsonNode resolved = milestone(milestoneId);
		assertThat(resolved.get("delayed").asBoolean()).isFalse();
		assertThat(resolved.get("incompleteTaskCount").asInt()).isZero();

		putJson(pm, moveBody(review, 1L), CARD_COLUMN, projectId, taskId).andExpect(status().isOk());
		assertThat(milestone(milestoneId).get("delayed").asBoolean()).isTrue();
	}

	// ---- 도우미 ----

	private String url(final String template) {
		return template.replace("{projectId}", String.valueOf(projectId));
	}

	private JsonNode columns(final Cookie cookie) throws Exception {
		return readResponse(getAs(cookie, BOARD, projectId)).get("columns");
	}

	private JsonNode milestone(final long milestoneId) throws Exception {
		return readResponse(getAs(pm, MILESTONES + "/{milestoneId}", projectId, milestoneId));
	}

	private long createTask(final String name, final Long parentTaskId, final Long assigneeUserId, final String end)
		throws Exception {
		return readResponse(postJson(url(WBS_TASKS), pm, taskBody(name, parentTaskId, assigneeUserId, end)))
			.get("taskId").asLong();
	}

	private ResultActions move(final Cookie cookie, final long taskId, final long columnId) throws Exception {
		final long version = taskRepository.findById(taskId).orElseThrow().getVersion();
		return putJson(cookie, moveBody(columnId, version), CARD_COLUMN, projectId, taskId).andExpect(status().isOk());
	}

	private static long lastColumnId(final JsonNode response) {
		final JsonNode columns = response.get("columns");
		return columns.get(columns.size() - 1).get("id").asLong();
	}

	private static Map<String, Object> taskBody(final String name, final Long parentTaskId, final Long assigneeUserId,
		final String end) {
		return body("parentTaskId", parentTaskId, "name", name, "assigneeUserId", assigneeUserId,
			"startDate", "2026-12-01", "endDate", end);
	}

	private static Map<String, Object> moveBody(final long columnId, final long version) {
		return body("columnId", columnId, "version", version);
	}

	private static Map<String, Object> columnBody(final String name, final String statusCategory, final Integer wipLimit) {
		return body("name", name, "statusCategory", statusCategory, "wipLimit", wipLimit);
	}

	private static List<Long> ids(final JsonNode nodes) {
		return nodes.valueStream().map(node -> node.get("id").asLong()).toList();
	}

	private static List<String> texts(final JsonNode nodes, final String field) {
		return nodes.valueStream().map(node -> node.get(field).asString()).toList();
	}
}
