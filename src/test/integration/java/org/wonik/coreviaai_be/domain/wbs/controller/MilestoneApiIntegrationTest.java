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
import org.springframework.test.web.servlet.ResultActions;
import org.wonik.coreviaai_be.domain.project.controller.ProjectApiTestSupport;
import org.wonik.coreviaai_be.domain.wbs.repository.MilestoneRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.MilestoneTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.service.MilestoneService;
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
 * FR-WBS-04 마일스톤을 HTTP 경계에서 검증한다.
 * <p>
 * 준비: 전사 공개 폭포수 프로젝트(2026-12-01 ~ 2027-03-31, WBS 기능 사용). 멤버 creator(PM)·developer(개발자)·
 * qa(QA)·businessOwner(현업 담당자). outsider는 멤버가 아닌 같은 부서 사용자, itExecutive는 소속 범위 경영진.
 * '오늘'은 테스트 Clock을 한국 시간(BusinessCalendar.ZONE) 기준으로 옮겨 정한다. 토큰도 Clock을 쓰므로 옮긴 뒤 다시 로그인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MilestoneApiIntegrationTest extends ProjectApiTestSupport {

	private static final String TASKS = PROJECTS + "/{projectId}/wbs/tasks";
	private static final String MILESTONES = PROJECTS + "/{projectId}/wbs/milestones";
	private static final String MILESTONE = MILESTONES + "/{milestoneId}";
	private static final String MILESTONE_TASKS = MILESTONE + "/tasks";

	@Autowired
	private MilestoneRepository milestoneRepository;
	@Autowired
	private MilestoneTaskRepository milestoneTaskRepository;
	@Autowired
	private WbsTaskRepository taskRepository;

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

	// ---- 등록·조회 ----

	@Test
	@DisplayName("등록: PM이 연결 작업과 함께 등록하면 MANUAL로 저장되고 목록은 목표일 순, 상세·목록 모두 연결 작업을 돌려준다")
	void createAndRead() throws Exception {
		final long design = createTask("설계", null, "2026-12-01", "2026-12-10");
		final long sub = createTask("API 설계", design, "2026-12-02", "2026-12-05");
		final long build = createTask("개발", null, "2026-12-11", "2027-01-31");

		// 중복 ID는 한 번만 연결
		final JsonNode open = readResponse(postJson(url(MILESTONES), pm,
			milestoneBody("  오픈  ", "2027-03-31", List.of(build, sub, build))));
		assertThat(open.get("name").asString()).isEqualTo("오픈");
		assertThat(open.get("source").asString()).isEqualTo("MANUAL");
		assertThat(open.get("version").asLong()).isZero();
		assertThat(open.get("linkedTaskCount").asInt()).isEqualTo(2);
		assertThat(taskIds(open)).containsExactly(sub, build);
		final JsonNode subNode = open.get("tasks").get(0);
		assertThat(subNode.get("parentTaskId").asLong()).isEqualTo(design);
		assertThat(subNode.get("name").asString()).isEqualTo("API 설계");
		assertThat(subNode.get("status").asString()).isEqualTo("TODO");
		assertThat(subNode.get("endDate").asString()).isEqualTo("2026-12-05");
		// 오늘(2026-11-09 KST)부터 2027-03-31까지 142일
		assertThat(open.get("dDay").asLong()).isEqualTo(142);
		assertThat(open.get("delayed").asBoolean()).isFalse();
		assertThat(open.get("incompleteTaskCount").asInt()).isEqualTo(2);

		final long designDone = readResponse(postJson(url(MILESTONES), pm,
			milestoneBody("설계 완료", "2026-12-10", null))).get("id").asLong();
		final long sameDate = readResponse(postJson(url(MILESTONES), pm,
			milestoneBody("설계 검토", "2026-12-10", List.of()))).get("id").asLong();

		final JsonNode list = milestones(pm);
		assertThat(ids(list)).containsExactly(designDone, sameDate, open.get("id").asLong());
		assertThat(list.get(0).get("tasks")).isEmpty();
		assertThat(list.get(0).get("linkedTaskCount").asInt()).isZero();
		assertThat(taskIds(list.get(2))).containsExactly(sub, build);

		final JsonNode detail = readResponse(getAs(pm, MILESTONE, projectId, open.get("id").asLong()));
		assertThat(detail.get("name").asString()).isEqualTo("오픈");
		assertThat(taskIds(detail)).containsExactly(sub, build);
		assertThat(milestoneTaskRepository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("조회 권한: 멤버(개발자·QA·현업 담당자)·공개 범위 열람자·소속 범위 경영진은 조회, 볼 수 없으면 404, 기능 꺼짐 404, 관리자 404")
	void readPermissions() throws Exception {
		final long milestoneId = createMilestone("설계 완료", "2026-12-10", List.of());

		for (final User viewer : List.of(developer, qa, businessOwner, outsider, itExecutive)) {
			final Cookie cookie = loginAs(viewer);
			assertThat(ids(milestones(cookie))).containsExactly(milestoneId);
			assertThat(readResponse(getAs(cookie, MILESTONE, projectId, milestoneId)).get("id").asLong())
				.isEqualTo(milestoneId);
		}

		final Map<String, Object> membersOnly = validCreateBody("MEM-2026");
		membersOnly.put("visibility", "MEMBERS_ONLY");
		final long membersOnlyId = createProject(membersOnly);
		expectError(getAs(loginAs(outsider), MILESTONES, membersOnlyId), ErrorCode.PROJECT_NOT_FOUND);
		expectError(getAs(loginAs(salesExecutive), MILESTONES, membersOnlyId), ErrorCode.PROJECT_NOT_FOUND);
		expectError(getAs(pm, MILESTONES, 999_999L), ErrorCode.PROJECT_NOT_FOUND);
		// 볼 수 없는 프로젝트에는 쓰기도 404(권한 403보다 먼저)
		expectError(postJson(MILESTONES.replace("{projectId}", String.valueOf(membersOnlyId)), loginAs(outsider),
			milestoneBody("마일스톤", "2026-12-10", null)), ErrorCode.PROJECT_NOT_FOUND);

		// 다른 프로젝트의 마일스톤 ID로 접근하면 404
		final long otherMilestone = readResponse(postJson(MILESTONES.replace("{projectId}", String.valueOf(membersOnlyId)),
			pm, milestoneBody("다른 프로젝트", "2026-12-10", null))).get("id").asLong();
		expectError(getAs(pm, MILESTONE, projectId, otherMilestone), ErrorCode.MILESTONE_NOT_FOUND);
		expectError(getAs(pm, MILESTONE, projectId, 999_999L), ErrorCode.MILESTONE_NOT_FOUND);

		final Map<String, Object> noWbs = validCreateBody("NOWBS-2026");
		noWbs.put("features", List.of("KANBAN"));
		final long noWbsId = createProject(noWbs);
		expectError(getAs(pm, MILESTONES, noWbsId), ErrorCode.FEATURE_DISABLED);
		expectError(postJson(MILESTONES.replace("{projectId}", String.valueOf(noWbsId)), pm,
			milestoneBody("마일스톤", "2026-12-10", null)), ErrorCode.FEATURE_DISABLED);

		saveUser("admin01", USER_PASSWORD, SystemRole.SYSTEM_ADMIN, null);
		expectError(getAs(loginOk("admin01", USER_PASSWORD), MILESTONES, projectId), ErrorCode.NOT_FOUND);
	}

	@Test
	@DisplayName("변경 권한: PM만 등록·수정·연결·삭제. 개발자·QA·현업 담당자·경영진·비멤버는 403 PROJECT_PM_ONLY이고 아무것도 바뀌지 않는다")
	void writePermissions() throws Exception {
		final long task = createTask("설계", null, "2026-12-01", "2026-12-10");
		final long milestoneId = createMilestone("설계 완료", "2026-12-10", List.of(task));

		for (final User user : List.of(developer, qa, businessOwner, itExecutive, outsider)) {
			final Cookie cookie = loginAs(user);
			expectError(postJson(url(MILESTONES), cookie, milestoneBody("마일스톤", "2026-12-10", null)),
				ErrorCode.PROJECT_PM_ONLY);
			expectError(patchJson(cookie, updateBody("변경", "2026-12-11", 0), MILESTONE, projectId, milestoneId),
				ErrorCode.PROJECT_PM_ONLY);
			expectError(putJson(cookie, linkBody(List.of(), 0), MILESTONE_TASKS, projectId, milestoneId),
				ErrorCode.PROJECT_PM_ONLY);
			expectError(deleteAs(cookie, MILESTONE, projectId, milestoneId), ErrorCode.PROJECT_PM_ONLY);
		}
		final JsonNode current = readResponse(getAs(pm, MILESTONE, projectId, milestoneId));
		assertThat(current.get("name").asString()).isEqualTo("설계 완료");
		assertThat(current.get("version").asLong()).isZero();
		assertThat(taskIds(current)).containsExactly(task);
		assertThat(milestoneRepository.count()).isEqualTo(1);
		assertThat(auditLogs(AuditAction.MILESTONE_DELETED)).isEmpty();
	}

	// ---- D-day·지연 경고 ----

	@Test
	@DisplayName("D-day·지연 경고: 한국 시간 기준 전날 1·당일 0·다음 날 -1, 미완료 연결 작업이 있으면 당일부터 경고하고 모두 완료하면 해제")
	void dDayAndDelay() throws Exception {
		final long done = createTask("완료 작업", null, "2026-12-01", "2026-12-10");
		final long open = createTask("진행 작업", null, "2026-12-01", "2026-12-20");
		setStatus(done, "DONE");
		final long mixed = createMilestone("설계 완료", "2026-12-20", List.of(done, open));
		final long allDone = createMilestone("분석 완료", "2026-12-20", List.of(done));
		final long noTasks = createMilestone("검토", "2026-12-20", List.of());

		// 한국 시간 12-19 23:59:59 (UTC 14:59:59): 전날
		today(LocalDateTime.of(2026, 12, 19, 23, 59, 59));
		JsonNode mixedNode = find(milestones(pm), mixed);
		assertThat(mixedNode.get("dDay").asLong()).isEqualTo(1);
		assertThat(mixedNode.get("delayed").asBoolean()).isFalse();
		assertThat(mixedNode.get("incompleteTaskCount").asInt()).isEqualTo(1);

		// 한국 시간 12-20 00:00 (UTC 12-19 15:00): 당일 → 경고
		today(LocalDateTime.of(2026, 12, 20, 0, 0));
		JsonNode list = milestones(pm);
		mixedNode = find(list, mixed);
		assertThat(mixedNode.get("dDay").asLong()).isZero();
		assertThat(mixedNode.get("delayed").asBoolean()).isTrue();
		assertThat(mixedNode.get("linkedTaskCount").asInt()).isEqualTo(2);
		assertThat(mixedNode.get("incompleteTaskCount").asInt()).isEqualTo(1);
		assertThat(find(list, allDone).get("delayed").asBoolean()).isFalse();
		assertThat(find(list, noTasks).get("delayed").asBoolean()).isFalse();
		assertThat(find(list, noTasks).get("linkedTaskCount").asInt()).isZero();

		// 다음 날: D+1, 상세 조회도 같은 계산
		today(LocalDateTime.of(2026, 12, 21, 9, 0));
		final JsonNode detail = readResponse(getAs(pm, MILESTONE, projectId, mixed));
		assertThat(detail.get("dDay").asLong()).isEqualTo(-1);
		assertThat(detail.get("delayed").asBoolean()).isTrue();

		// 남은 작업을 완료하면 경고 해제(저장값이 아니라 조회 때 계산)
		setStatus(open, "DONE");
		final JsonNode resolved = readResponse(getAs(pm, MILESTONE, projectId, mixed));
		assertThat(resolved.get("delayed").asBoolean()).isFalse();
		assertThat(resolved.get("incompleteTaskCount").asInt()).isZero();
		// 리뷰 상태도 미완료
		setStatus(open, "REVIEW");
		assertThat(readResponse(getAs(pm, MILESTONE, projectId, mixed)).get("delayed").asBoolean()).isTrue();
	}

	// ---- 수정 ----

	@Test
	@DisplayName("수정: 이름·목표일을 바꾸면 version이 오르고, 오래된 version은 409, 같은 값 재전송은 version과 관계없이 200")
	void updateWithOptimisticLock() throws Exception {
		final long milestoneId = createMilestone("설계 완료", "2026-12-10", List.of());

		final JsonNode updated = readResponse(patchJson(pm, updateBody(" 설계 종료 ", "2026-12-15", 0),
			MILESTONE, projectId, milestoneId));
		assertThat(updated.get("name").asString()).isEqualTo("설계 종료");
		assertThat(updated.get("targetDate").asString()).isEqualTo("2026-12-15");
		assertThat(updated.get("version").asLong()).isEqualTo(1);

		// 다른 사용자가 먼저 고친 뒤의 오래된 version
		expectError(patchJson(pm, updateBody("다른 이름", "2026-12-15", 0), MILESTONE, projectId, milestoneId),
			ErrorCode.CONCURRENT_MODIFICATION);
		// 같은 값 재전송(멱등)
		assertThat(readResponse(patchJson(pm, updateBody("설계 종료", "2026-12-15", 0), MILESTONE, projectId,
			milestoneId)).get("version").asLong()).isEqualTo(1);
		assertThat(milestoneRepository.findById(milestoneId).orElseThrow().getName()).isEqualTo("설계 종료");
		expectError(patchJson(pm, updateBody("변경", "2026-12-15", 1), MILESTONE, projectId, 999_999L),
			ErrorCode.MILESTONE_NOT_FOUND);
	}

	@Test
	@DisplayName("입력 경계: 목표일은 프로젝트 기간 안(시작일·종료일 당일 허용), 이름 공백·201자·목표일·version 누락은 400")
	void inputBoundaries() throws Exception {
		expectError(postJson(url(MILESTONES), pm, milestoneBody("이른 날", "2026-11-30", null)),
			ErrorCode.INVALID_MILESTONE_TARGET_DATE);
		expectError(postJson(url(MILESTONES), pm, milestoneBody("늦은 날", "2027-04-01", null)),
			ErrorCode.INVALID_MILESTONE_TARGET_DATE);
		final long first = createMilestone("시작일", "2026-12-01", null);
		createMilestone("x".repeat(200), "2027-03-31", null);

		expectBadRequest(postJson(url(MILESTONES), pm, milestoneBody("   ", "2026-12-10", null)));
		expectBadRequest(postJson(url(MILESTONES), pm, milestoneBody("x".repeat(201), "2026-12-10", null)));
		expectBadRequest(postJson(url(MILESTONES), pm, milestoneBody("목표일 없음", null, null)));
		expectBadRequest(postJson(url(MILESTONES), pm, body("name", "형식", "targetDate", "2026-13-01")));
		expectBadRequest(postJson(url(MILESTONES), pm, milestoneBody("음수 ID", "2026-12-10", List.of(-1L))));
		expectBadRequest(postJson(url(MILESTONES), pm, milestoneBody("null ID", "2026-12-10",
			Collections.singletonList(null))));
		expectBadRequest(patchJson(pm, body("name", "버전 없음", "targetDate", "2026-12-10"),
			MILESTONE, projectId, first));
		expectBadRequest(patchJson(pm, updateBody("음수 버전", "2026-12-10", -1), MILESTONE, projectId, first));
		expectError(patchJson(pm, updateBody("시작일", "2027-04-01", 0), MILESTONE, projectId, first),
			ErrorCode.INVALID_MILESTONE_TARGET_DATE);
		assertThat(milestoneRepository.count()).isEqualTo(2);
	}

	// ---- 연결 작업 ----

	@Test
	@DisplayName("연결 교체: 보낸 목록이 새 연결 전체(빠진 연결 삭제·새 연결 추가), version +1. 같은 목록은 멱등, 오래된 version은 409, 빈 목록은 모두 해제")
	void replaceTasks() throws Exception {
		final long a = createTask("A", null, "2026-12-01", "2026-12-10");
		final long b = createTask("B", null, "2026-12-01", "2026-12-10");
		final long c = createTask("C", null, "2026-12-01", "2026-12-10");
		final long milestoneId = createMilestone("설계 완료", "2026-12-10", List.of(a, b));
		final long keptLinkId = milestoneTaskRepository.findAllByMilestoneId(milestoneId).stream()
			.filter(link -> link.getTaskId() == a).findFirst().orElseThrow().getId();

		final JsonNode replaced = readResponse(putJson(pm, linkBody(List.of(c, a, c), 0), MILESTONE_TASKS, projectId,
			milestoneId));
		assertThat(taskIds(replaced)).containsExactly(a, c);
		assertThat(replaced.get("version").asLong()).isEqualTo(1);
		// 남는 연결(A)은 그대로 두고 B만 지우고 C만 추가
		assertThat(milestoneTaskRepository.findAllByMilestoneId(milestoneId))
			.extracting(link -> link.getTaskId()).containsExactlyInAnyOrder(a, c);
		assertThat(milestoneTaskRepository.findById(keptLinkId)).isPresent();
		assertThat(milestoneRepository.findById(milestoneId).orElseThrow().getVersion()).isEqualTo(1);

		// 같은 목록 재전송: 오래된 version이어도 200, version 그대로
		assertThat(readResponse(putJson(pm, linkBody(List.of(a, c), 0), MILESTONE_TASKS, projectId, milestoneId))
			.get("version").asLong()).isEqualTo(1);
		// 다른 목록인데 오래된 version → 409, 변경 없음
		expectError(putJson(pm, linkBody(List.of(b), 0), MILESTONE_TASKS, projectId, milestoneId),
			ErrorCode.CONCURRENT_MODIFICATION);
		assertThat(taskIds(readResponse(getAs(pm, MILESTONE, projectId, milestoneId)))).containsExactly(a, c);

		// 연결 변경 뒤에는 이름 수정도 새 version이 필요(같은 version 체계)
		expectError(patchJson(pm, updateBody("변경", "2026-12-10", 0), MILESTONE, projectId, milestoneId),
			ErrorCode.CONCURRENT_MODIFICATION);

		final JsonNode cleared = readResponse(putJson(pm, linkBody(List.of(), 1), MILESTONE_TASKS, projectId,
			milestoneId));
		assertThat(cleared.get("tasks")).isEmpty();
		assertThat(cleared.get("version").asLong()).isEqualTo(2);
		assertThat(milestoneTaskRepository.count()).isZero();

		expectBadRequest(putJson(pm, body("taskIds", null, "version", 2), MILESTONE_TASKS, projectId, milestoneId));
		expectBadRequest(putJson(pm, body("taskIds", List.of(a)), MILESTONE_TASKS, projectId, milestoneId));
		expectError(putJson(pm, linkBody(List.of(a), 2), MILESTONE_TASKS, projectId, 999_999L),
			ErrorCode.MILESTONE_NOT_FOUND);
	}

	@Test
	@DisplayName("연결 거부: 다른 프로젝트 작업·없는 작업은 400 INVALID_MILESTONE_TASK(등록·교체 모두), 201개는 400, 200개는 허용")
	void rejectForeignTasks() throws Exception {
		final long own = createTask("내 작업", null, "2026-12-01", "2026-12-10");
		final long otherProjectId = createProject(validCreateBody("OTHER-2026"));
		final long foreign = readResponse(postJson(TASKS.replace("{projectId}", String.valueOf(otherProjectId)), pm,
			body("parentTaskId", null, "name", "다른 프로젝트 작업", "startDate", "2026-12-01", "endDate", "2026-12-02")))
			.get("taskId").asLong();

		expectError(postJson(url(MILESTONES), pm, milestoneBody("마일스톤", "2026-12-10", List.of(own, foreign))),
			ErrorCode.INVALID_MILESTONE_TASK);
		expectError(postJson(url(MILESTONES), pm, milestoneBody("마일스톤", "2026-12-10", List.of(999_999L))),
			ErrorCode.INVALID_MILESTONE_TASK);
		assertThat(milestoneRepository.count()).isZero();

		final long milestoneId = createMilestone("마일스톤", "2026-12-10", List.of(own));
		expectError(putJson(pm, linkBody(List.of(own, foreign), 0), MILESTONE_TASKS, projectId, milestoneId),
			ErrorCode.INVALID_MILESTONE_TASK);
		assertThat(taskIds(readResponse(getAs(pm, MILESTONE, projectId, milestoneId)))).containsExactly(own);

		// 개수 상한(요청 형식): 201개는 400, 200개는 허용
		final List<Long> taskIds = insertTasks(200);
		final List<Long> tooMany = new ArrayList<>(taskIds);
		tooMany.add(own);
		expectBadRequest(putJson(pm, linkBody(tooMany, 0), MILESTONE_TASKS, projectId, milestoneId));
		assertThat(readResponse(putJson(pm, linkBody(taskIds, 0), MILESTONE_TASKS, projectId, milestoneId))
			.get("linkedTaskCount").asInt()).isEqualTo(200);
	}

	// ---- 삭제·상한 ----

	@Test
	@DisplayName("삭제: PM이 지우면 연결 정보도 함께 지워지고 작업은 남아 삭제 가능해진다. 감사 로그를 남기고 다시 지우면 404")
	void delete() throws Exception {
		final long task = createTask("설계", null, "2026-12-01", "2026-12-10");
		final long milestoneId = createMilestone("설계 완료", "2026-12-10", List.of(task));
		final long other = createMilestone("오픈", "2027-03-31", List.of(task));
		// 연결된 작업은 지울 수 없다(SCRUM-107)
		expectError(deleteAs(pm, TASKS + "/{taskId}", projectId, task), ErrorCode.WBS_TASK_IN_USE);

		readResponse(deleteAs(pm, MILESTONE, projectId, milestoneId));
		assertThat(milestoneRepository.existsById(milestoneId)).isFalse();
		assertThat(milestoneTaskRepository.findAllByMilestoneId(milestoneId)).isEmpty();
		assertThat(taskIds(readResponse(getAs(pm, MILESTONE, projectId, other)))).containsExactly(task);
		assertThat(taskRepository.existsById(task)).isTrue();
		expectError(deleteAs(pm, MILESTONE, projectId, milestoneId), ErrorCode.MILESTONE_NOT_FOUND);

		final List<AuditLog> logs = auditLogs(AuditAction.MILESTONE_DELETED);
		assertThat(logs).hasSize(1);
		assertThat(logs.get(0).getTargetType()).isEqualTo(AuditTargetType.MILESTONE);
		assertThat(logs.get(0).getTargetId()).isEqualTo(milestoneId);
		assertThat(logs.get(0).getActorUserId()).isEqualTo(creator.getId());
		assertThat(logs.get(0).getDetails()).contains("\"name\":\"설계 완료\"", "\"targetDate\":\"2026-12-10\"",
			"\"unlinkedTaskCount\":1");

		// 남은 마일스톤도 지우면 작업 삭제 가능
		readResponse(deleteAs(pm, MILESTONE, projectId, other));
		readResponse(deleteAs(pm, TASKS + "/{taskId}", projectId, task));
		assertThat(taskRepository.existsById(task)).isFalse();
	}

	@Test
	@DisplayName("상한: 프로젝트당 마일스톤 200개까지 등록, 201번째는 409 MILESTONE_LIMIT_EXCEEDED")
	void milestoneLimit() throws Exception {
		final List<Object[]> rows = new ArrayList<>();
		for (int i = 1; i < MilestoneService.MAX_MILESTONES_PER_PROJECT; i++) {
			rows.add(new Object[] {projectId, creator.getId()});
		}
		jdbcTemplate.batchUpdate("""
			INSERT INTO milestones (project_id, name, target_date, source, created_by, created_at, updated_at)
			VALUES (?, '마일스톤', DATE '2026-12-20', 'MANUAL', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
			""", rows);

		createMilestone("200번째", "2026-12-20", null);
		expectError(postJson(url(MILESTONES), pm, milestoneBody("201번째", "2026-12-20", null)),
			ErrorCode.MILESTONE_LIMIT_EXCEEDED);
		assertThat(milestones(pm)).hasSize(MilestoneService.MAX_MILESTONES_PER_PROJECT);
	}

	// ---- 도우미 ----

	private String url(final String template) {
		return template.replace("{projectId}", String.valueOf(projectId));
	}

	/** 한국 시간 기준 '지금'을 옮기고 PM으로 다시 로그인한다. */
	private void today(final LocalDateTime koreanTime) throws Exception {
		clock.setInstant(koreanTime.atZone(BusinessCalendar.ZONE).toInstant());
		pm = loginAs(creator);
	}

	private void setStatus(final long taskId, final String status) {
		jdbcTemplate.update("UPDATE wbs_tasks SET status = ? WHERE id = ?", status, taskId);
	}

	private long createTask(final String name, final Long parentTaskId, final String start, final String end)
		throws Exception {
		return readResponse(postJson(url(TASKS), pm, body("parentTaskId", parentTaskId, "name", name,
			"startDate", start, "endDate", end))).get("taskId").asLong();
	}

	private List<Long> insertTasks(final int count) {
		final List<Object[]> rows = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			rows.add(new Object[] {projectId, 100 + i, creator.getId()});
		}
		jdbcTemplate.batchUpdate("""
			INSERT INTO wbs_tasks (project_id, sort_order, name, start_date, end_date, status, source, created_by,
			                       created_at, updated_at)
			VALUES (?, ?, '작업', DATE '2026-12-01', DATE '2026-12-02', 'TODO', 'MANUAL', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
			""", rows);
		return jdbcTemplate.queryForList("SELECT id FROM wbs_tasks WHERE project_id = ? AND sort_order > 100 ORDER BY id",
			Long.class, projectId);
	}

	private long createMilestone(final String name, final String targetDate, final List<Long> taskIds) throws Exception {
		return readResponse(postJson(url(MILESTONES), pm, milestoneBody(name, targetDate, taskIds))).get("id").asLong();
	}

	private JsonNode milestones(final Cookie cookie) throws Exception {
		return readResponse(getAs(cookie, MILESTONES, projectId)).get("milestones");
	}

	private static JsonNode find(final JsonNode list, final long milestoneId) {
		return list.valueStream().filter(node -> node.get("id").asLong() == milestoneId).findFirst()
			.orElseThrow(() -> new AssertionError("목록에 없는 마일스톤: " + milestoneId));
	}

	private static void expectBadRequest(final ResultActions actions) throws Exception {
		actions.andExpect(status().isBadRequest());
	}

	private static Map<String, Object> milestoneBody(final String name, final String targetDate, final List<Long> taskIds) {
		return body("name", name, "targetDate", targetDate, "taskIds", taskIds);
	}

	private static Map<String, Object> updateBody(final String name, final String targetDate, final long version) {
		return body("name", name, "targetDate", targetDate, "version", version);
	}

	private static Map<String, Object> linkBody(final List<Long> taskIds, final long version) {
		return body("taskIds", taskIds, "version", version);
	}

	private static List<Long> ids(final JsonNode nodes) {
		return nodes.valueStream().map(node -> node.get("id").asLong()).toList();
	}

	private static List<Long> taskIds(final JsonNode milestone) {
		return ids(milestone.get("tasks"));
	}
}
