package org.wonik.coreviaai_be.domain.wbs.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.wonik.coreviaai_be.domain.project.controller.ProjectApiTestSupport;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.WaterfallPhaseType;
import org.wonik.coreviaai_be.domain.wbs.client.WbsDraftAiClient;
import org.wonik.coreviaai_be.domain.wbs.entity.Milestone;
import org.wonik.coreviaai_be.domain.wbs.entity.MilestoneTask;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsDependency;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsTaskStatus;
import org.wonik.coreviaai_be.domain.aidraft.entity.AiDraft;
import org.wonik.coreviaai_be.domain.aidraft.entity.type.AiDraftFeature;
import org.wonik.coreviaai_be.domain.aidraft.entity.type.AiDraftStatus;
import org.wonik.coreviaai_be.domain.aidraft.repository.AiDraftRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.MilestoneRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.MilestoneTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsDependencyRepository;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.coreviaai_be.domain.wbs.service.MilestoneService;
import org.wonik.coreviaai_be.domain.wbs.service.WbsTaskService;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftTeamMember;
import org.wonik.coreviaai_be.domain.wbs.service.command.WbsDraftContext;
import org.wonik.coreviaai_be.domain.wbs.service.result.DraftFeasibility;
import org.wonik.coreviaai_be.domain.wbs.service.result.ProposedMilestone;
import org.wonik.coreviaai_be.domain.wbs.service.result.ProposedStage;
import org.wonik.coreviaai_be.domain.wbs.service.result.ProposedSubtask;
import org.wonik.coreviaai_be.domain.wbs.service.result.ProposedTask;
import org.wonik.coreviaai_be.domain.wbs.service.result.WbsDraftProposal;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditLog;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * FR-PRJ-07 AI WBS 초안 생성·반영을 HTTP 경계에서 검증한다. aiserver 호출(WbsDraftAiClient)만 가짜로 바꾼다.
 * <p>
 * 준비: 전사 공개 폭포수 프로젝트(2026-12-01 ~ 2027-03-31, WBS 기능 사용). 멤버 creator(PM)·developer(개발자)·
 * businessOwner(현업 담당자). 반영한 작업은 단계와 연결하지 않는다(04. WBS·일정 ERD).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WbsAiDraftApiIntegrationTest extends ProjectApiTestSupport {

	private static final String AI_DRAFT = PROJECTS + "/{id}/wbs/ai-draft";
	private static final String APPLY = AI_DRAFT + "/apply";

	@MockitoBean
	private WbsDraftAiClient aiClient;
	@Autowired
	private WbsTaskRepository taskRepository;
	@Autowired
	private WbsDependencyRepository dependencyRepository;
	@Autowired
	private MilestoneRepository milestoneRepository;
	@Autowired
	private MilestoneTaskRepository milestoneTaskRepository;
	@Autowired
	private AiDraftRepository aiDraftRepository;

	private long projectId;
	private Cookie pm;

	@BeforeEach
	void createProject() throws Exception {
		projectId = createProject(validCreateBody("PUR-2026"));
		pm = loginAs(creator);
		when(aiClient.generate(any())).thenReturn(proposal());
	}

	// ---- 생성 ----

	@Test
	@DisplayName("생성: PM이 요청하면 WBS는 바꾸지 않고 'AI 초안'과 반영 토큰을 돌려주며, AI 결과를 ai_drafts에 DRAFT로 남긴다")
	void generate() throws Exception {
		final JsonNode draft = readResponse(generate(pm, body("projectPurpose", "구매 발주를 전산화한다",
			"requirementsText", "발주 등록, 승인, 조회", "constraints", List.of("12월 오픈"))));

		assertThat(draft.get("status").asString()).isEqualTo("AI_DRAFT");
		assertThat(draft.get("draftToken").asString()).isNotBlank();
		assertThat(draft.get("expiresAt").asString()).isEqualTo("2026-11-09T01:00:00Z");
		assertThat(draft.get("stages").get(0).get("stageCode").asString()).isEqualTo("ANALYSIS");
		assertThat(draft.get("stages").get(0).get("tasks").get(0).get("tempId").asString()).isEqualTo("T-001");
		assertThat(draft.get("milestones").get(0).get("tempId").asString()).isEqualTo("M-001");
		assertThat(draft.get("feasibility").get("status").asString()).isEqualTo("AT_RISK");
		assertThat(taskRepository.count()).isZero();

		final AiDraft aiDraft = singleAiDraft();
		assertThat(aiDraft.getFeature()).isEqualTo(AiDraftFeature.WBS_DRAFT);
		assertThat(aiDraft.getStatus()).isEqualTo(AiDraftStatus.DRAFT);
		assertThat(aiDraft.getProjectId()).isEqualTo(projectId);
		assertThat(aiDraft.getRequestedBy()).isEqualTo(creator.getId());
		assertThat(aiDraft.getOutput()).contains("\"T-001\"", "\"M-001\"").doesNotContain("발주 등록, 승인, 조회");
		assertThat(aiDraft.getDecidedBy()).isNull();

		final ArgumentCaptor<WbsDraftContext> context = ArgumentCaptor.forClass(WbsDraftContext.class);
		verify(aiClient).generate(context.capture());
		assertThat(context.getValue().phases()).containsExactly(WaterfallPhaseType.values());
		assertThat(context.getValue().members()).extracting(DraftTeamMember::userId, DraftTeamMember::actor)
			.containsExactly(
				tuple(creator.getId(), ProjectActor.PM),
				tuple(developer.getId(), ProjectActor.DEVELOPER),
				tuple(businessOwner.getId(), ProjectActor.BUSINESS_OWNER));
		assertThat(context.getValue().command().constraints()).containsExactly("12월 오픈");

		final List<AuditLog> logs = auditLogs(AuditAction.AI_WBS_DRAFT_REQUESTED);
		assertThat(logs).hasSize(1);
		assertThat(logs.get(0).getDetails())
			.contains("\"aiDraftId\":" + aiDraft.getId(), "\"outcome\":\"SUCCESS\"", "\"proposedTaskCount\":2", "\"proposedMilestoneCount\":1")
			.doesNotContain("구매 발주를 전산화한다");
	}

	@Test
	@DisplayName("생성: PM이 아니면(멤버·경영진) 403 PROJECT_PM_ONLY이고 AI를 부르지 않는다")
	void generateByNonPm() throws Exception {
		expectError(generate(loginAs(developer), validGenerateBody()), ErrorCode.PROJECT_PM_ONLY);
		expectError(generate(loginAs(itExecutive), validGenerateBody()), ErrorCode.PROJECT_PM_ONLY);

		verify(aiClient, never()).generate(any());
	}

	@Test
	@DisplayName("생성: 목적과 요구사항이 모두 비어 있으면 400")
	void generateWithoutInput() throws Exception {
		generate(pm, body("projectPurpose", " ", "requirementsText", null)).andExpect(status().isBadRequest());

		verify(aiClient, never()).generate(any());
	}

	@Test
	@DisplayName("생성: AI 사용 불가는 503으로 알리고, 실패한 요청도 ai_drafts(FAILED)와 감사 로그에 남긴다")
	void generateWhenAiUnavailable() throws Exception {
		when(aiClient.generate(any())).thenThrow(new CustomException(ErrorCode.AI_NOT_READY));

		expectError(generate(pm, validGenerateBody()), ErrorCode.AI_NOT_READY);

		assertThat(auditLogs(AuditAction.AI_WBS_DRAFT_REQUESTED)).singleElement()
			.extracting(AuditLog::getDetails).asString().contains("\"outcome\":\"AI_NOT_READY\"");
		final AiDraft aiDraft = singleAiDraft();
		assertThat(aiDraft.getStatus()).isEqualTo(AiDraftStatus.FAILED);
		assertThat(aiDraft.getOutput()).isNull();
	}

	// ---- 반영 ----

	@Test
	@DisplayName("반영: 승인한 작업·하위작업·선후행·마일스톤만 WBS에 저장하고 tempId별 정식 ID를 돌려준다")
	void apply() throws Exception {
		final String token = generateToken();

		final JsonNode applied = readResponse(apply(pm, validApplyBody(token)));

		final long analysisTaskId = applied.get("taskIds").get("T-001").asLong();
		final long subtaskId = applied.get("taskIds").get("ST-001").asLong();
		final long designTaskId = applied.get("taskIds").get("T-002").asLong();
		final long milestoneId = applied.get("milestoneIds").get("M-001").asLong();

		final WbsTask analysis = taskRepository.findById(analysisTaskId).orElseThrow();
		assertThat(analysis.getParentTaskId()).isNull();
		assertThat(analysis.getAssigneeUserId()).isEqualTo(developer.getId());
		assertThat(analysis.getEstimatedEffort()).isEqualByComparingTo("3.5");
		assertThat(analysis.getSource()).isEqualTo(WbsItemSource.AI_DRAFT);
		assertThat(analysis.getStatus()).isEqualTo(WbsTaskStatus.TODO);
		assertThat(analysis.getSortOrder()).isEqualTo(1);
		assertThat(taskRepository.findById(designTaskId).orElseThrow().getSortOrder()).isEqualTo(2);

		final WbsTask subtask = taskRepository.findById(subtaskId).orElseThrow();
		assertThat(subtask.getParentTaskId()).isEqualTo(analysisTaskId);
		assertThat(subtask.getSortOrder()).isEqualTo(1);
		assertThat(subtask.getStartDate()).isEqualTo(LocalDate.parse("2026-12-01"));
		assertThat(subtask.getEndDate()).isEqualTo(LocalDate.parse("2026-12-10"));

		assertThat(dependencyRepository.findAll()).singleElement()
			.extracting(WbsDependency::getPredecessorTaskId, WbsDependency::getSuccessorTaskId)
			.containsExactly(analysisTaskId, designTaskId);
		assertThat(milestoneRepository.findById(milestoneId)).get()
			.extracting(Milestone::getName).isEqualTo("설계 완료");
		assertThat(milestoneTaskRepository.findAll()).singleElement()
			.extracting(MilestoneTask::getTaskId).isEqualTo(designTaskId);

		assertThat(auditLogs(AuditAction.AI_WBS_DRAFT_APPLIED)).singleElement()
			.extracting(AuditLog::getDetails).asString()
			.contains("\"taskCount\":2", "\"subtaskCount\":1", "\"milestoneCount\":1", "\"rejectedCount\":1");

		// 제외한 항목이 있어 일부 수락으로 남는다.
		final AiDraft aiDraft = singleAiDraft();
		assertThat(aiDraft.getStatus()).isEqualTo(AiDraftStatus.PARTIALLY_ACCEPTED);
		assertThat(aiDraft.getDecidedBy()).isEqualTo(creator.getId());
		assertThat(aiDraft.getDecidedAt()).isNotNull();
	}

	@Test
	@DisplayName("반영: 하위작업 기간이 있으면 상위 작업 기간을 하위작업 기간으로 맞춘다(FR-WBS-01 상위 기간 자동 산출)")
	void applyDerivesParentSchedule() throws Exception {
		final Map<String, Object> body = body(
			"draftToken", generateToken(),
			"tasks", List.of(applyTask("T-001", "2026-12-01", "2026-12-20", null, List.of(), List.of(
				body("tempId", "ST-001", "name", "앞", "startDate", "2026-12-03", "endDate", "2026-12-05"),
				body("tempId", "ST-002", "name", "뒤", "startDate", "2026-12-04", "endDate", "2026-12-12")))),
			"milestones", List.of(),
			"rejectedCount", 0
		);

		final long taskId = readResponse(apply(pm, body)).get("taskIds").get("T-001").asLong();

		final WbsTask task = taskRepository.findById(taskId).orElseThrow();
		assertThat(task.getStartDate()).isEqualTo(LocalDate.parse("2026-12-03"));
		assertThat(task.getEndDate()).isEqualTo(LocalDate.parse("2026-12-12"));
	}

	@Test
	@DisplayName("반영: 반영 후 프로젝트 WBS 항목이 2,000개를 넘으면 409 WBS_TASK_LIMIT_EXCEEDED이고 아무것도 저장하지 않는다")
	void applyOverItemLimit() throws Exception {
		final List<Object[]> rows = new ArrayList<>();
		for (int i = 1; i <= WbsTaskService.MAX_ITEMS_PER_PROJECT - 2; i++) {
			rows.add(new Object[] {projectId, i, creator.getId()});
		}
		jdbcTemplate.batchUpdate("""
			INSERT INTO wbs_tasks (project_id, sort_order, name, start_date, end_date, status, source, created_by,
			                       created_at, updated_at)
			VALUES (?, ?, '작업', DATE '2026-12-01', DATE '2026-12-02', 'TODO', 'MANUAL', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
			""", rows);

		// 작업 2 + 하위작업 1 = 3개 추가 → 2,001개
		expectError(apply(pm, validApplyBody(generateToken())), ErrorCode.WBS_TASK_LIMIT_EXCEEDED);
		assertThat(taskRepository.count()).isEqualTo(WbsTaskService.MAX_ITEMS_PER_PROJECT - 2);
	}

	@Test
	@DisplayName("반영: 반영 후 프로젝트 마일스톤이 200개를 넘으면 409 MILESTONE_LIMIT_EXCEEDED이고 아무것도 저장하지 않는다(SCRUM-108)")
	void applyOverMilestoneLimit() throws Exception {
		final List<Object[]> rows = new ArrayList<>();
		for (int i = 1; i <= MilestoneService.MAX_MILESTONES_PER_PROJECT; i++) {
			rows.add(new Object[] {projectId, creator.getId()});
		}
		jdbcTemplate.batchUpdate("""
			INSERT INTO milestones (project_id, name, target_date, source, created_by, created_at, updated_at)
			VALUES (?, '마일스톤', DATE '2026-12-20', 'MANUAL', ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
			""", rows);

		// 마일스톤 1개 추가 → 201개
		expectError(apply(pm, validApplyBody(generateToken())), ErrorCode.MILESTONE_LIMIT_EXCEEDED);
		assertThat(milestoneRepository.count()).isEqualTo(MilestoneService.MAX_MILESTONES_PER_PROJECT);
		assertThat(taskRepository.count()).isZero();
	}

	@Test
	@DisplayName("반영: 같은 토큰으로 다시 보내면 409 AI_DRAFT_ALREADY_APPLIED이고 WBS는 한 번만 만들어진다")
	void applyTwice() throws Exception {
		final String token = generateToken();
		apply(pm, validApplyBody(token)).andExpect(status().isOk());

		expectError(apply(pm, validApplyBody(token)), ErrorCode.AI_DRAFT_ALREADY_APPLIED);

		assertThat(taskRepository.count()).isEqualTo(3);
	}

	@Test
	@DisplayName("반영: 검증에 실패하면 아무것도 저장하지 않고, 고친 뒤 같은 토큰으로 다시 반영할 수 있다")
	void applyAfterFixingInvalidSchedule() throws Exception {
		final String token = generateToken();
		final Map<String, Object> invalid = body(
			"draftToken", token,
			"tasks", List.of(applyTask("T-001", "2026-11-30", "2026-12-10", null, List.of(), List.of())),
			"milestones", List.of(),
			"rejectedCount", 0
		);

		expectError(apply(pm, invalid), ErrorCode.INVALID_WBS_DRAFT_SCHEDULE);
		assertThat(taskRepository.count()).isZero();
		assertThat(singleAiDraft().getStatus()).isEqualTo(AiDraftStatus.DRAFT);

		apply(pm, validApplyBody(token)).andExpect(status().isOk());
		assertThat(taskRepository.count()).isEqualTo(3);
	}

	@Test
	@DisplayName("반영: 멤버가 아닌 담당자를 지정하면 400 INVALID_TASK_ASSIGNEE")
	void applyWithNonMemberAssignee() throws Exception {
		final Map<String, Object> body = body(
			"draftToken", generateToken(),
			"tasks", List.of(applyTask("T-001", "2026-12-01", "2026-12-10", outsider.getId(), List.of(),
				List.of())),
			"milestones", List.of(),
			"rejectedCount", 0
		);

		expectError(apply(pm, body), ErrorCode.INVALID_TASK_ASSIGNEE);
	}

	@Test
	@DisplayName("반영: 토큰 유효 시간(1시간)이 지나면 404 AI_DRAFT_EXPIRED")
	void applyExpiredToken() throws Exception {
		final String token = generateToken();
		clock.advance(Duration.ofHours(1));
		pm = loginAs(creator); // 로그인 세션(30분)도 함께 만료되므로 다시 로그인한다

		expectError(apply(pm, validApplyBody(token)), ErrorCode.AI_DRAFT_EXPIRED);
	}

	@Test
	@DisplayName("반영: 다른 프로젝트에서 받은 토큰이나 없는 토큰은 404 AI_DRAFT_EXPIRED")
	void applyTokenOfOtherProject() throws Exception {
		final long otherProjectId = createProject(validCreateBody("ERP-2026"));
		final String token = generateToken();

		expectError(apply(pm, otherProjectId, validApplyBody(token)), ErrorCode.AI_DRAFT_EXPIRED);
		expectError(apply(pm, validApplyBody("unknown-token")), ErrorCode.AI_DRAFT_EXPIRED);
	}

	@Test
	@DisplayName("반영: PM이 아니면 403 PROJECT_PM_ONLY이고 토큰은 그대로 남아 PM이 반영할 수 있다")
	void applyByNonPm() throws Exception {
		final String token = generateToken();

		expectError(apply(loginAs(developer), validApplyBody(token)), ErrorCode.PROJECT_PM_ONLY);

		apply(pm, validApplyBody(token)).andExpect(status().isOk());
	}

	@Test
	@DisplayName("반영: 승인 항목이 하나도 없으면 400이고 토큰은 소비되지 않는다")
	void applyNothing() throws Exception {
		final String token = generateToken();

		apply(pm, body("draftToken", token, "tasks", List.of(), "milestones", List.of(), "rejectedCount", 3))
			.andExpect(status().isBadRequest());

		apply(pm, validApplyBody(token)).andExpect(status().isOk());
	}

	@Test
	@DisplayName("기간 없는 프로젝트: 기간을 비운 채 AI에 요청하고, 반영할 때 날짜를 프로젝트 기간으로 제한하지 않는다")
	void projectWithoutPeriod() throws Exception {
		final Map<String, Object> noPeriod = new HashMap<>(validCreateBody("NOP-2026"));
		noPeriod.remove("startDate");
		noPeriod.remove("endDate");
		projectId = createProject(noPeriod);

		final String token = generateToken();

		final ArgumentCaptor<WbsDraftContext> context = ArgumentCaptor.forClass(WbsDraftContext.class);
		verify(aiClient).generate(context.capture());
		assertThat(context.getValue().startDate()).isNull();
		assertThat(context.getValue().endDate()).isNull();

		final Map<String, Object> farFuture = body(
			"draftToken", token,
			"tasks", List.of(applyTask("T-001", "2030-01-01", "2030-12-31", null, List.of(), List.of())),
			"milestones", List.of(),
			"rejectedCount", 0
		);
		apply(pm, farFuture).andExpect(status().isOk());
		assertThat(taskRepository.count()).isEqualTo(1);
		// 제외 항목 없이 반영하면 전부 수락으로 남는다.
		assertThat(singleAiDraft().getStatus()).isEqualTo(AiDraftStatus.ACCEPTED);
	}

	@Test
	@DisplayName("단계 없음 프로젝트는 404 PHASES_NOT_AVAILABLE이고 AI를 부르지 않는다")
	void projectWithoutPhases() throws Exception {
		final Map<String, Object> none = new HashMap<>(validCreateBody("NON-2026"));
		none.put("template", "EMPTY");
		none.put("methodology", "NONE");
		none.put("features", List.of("WBS", "KANBAN"));
		none.remove("gateApproverUserId");
		projectId = createProject(none);

		expectError(generate(pm, validGenerateBody()), ErrorCode.PHASES_NOT_AVAILABLE);
		verify(aiClient, never()).generate(any());
	}

	@Test
	@DisplayName("WBS 기능을 끈 프로젝트는 생성·반영 모두 404 FEATURE_DISABLED")
	void wbsFeatureDisabled() throws Exception {
		final Map<String, Object> noWbs = new HashMap<>(validCreateBody("NOW-2026"));
		noWbs.put("features", List.of("KANBAN", "DELIVERABLE"));
		projectId = createProject(noWbs);

		expectError(generate(pm, validGenerateBody()), ErrorCode.FEATURE_DISABLED);
		expectError(apply(pm, validApplyBody("any-token")), ErrorCode.FEATURE_DISABLED);
		verify(aiClient, never()).generate(any());
	}

	// ---- 준비 ----

	private ResultActions generate(final Cookie cookie, final Map<String, Object> body) throws Exception {
		return postJson(AI_DRAFT.replace("{id}", String.valueOf(projectId)), cookie, body);
	}

	private ResultActions apply(final Cookie cookie, final Map<String, Object> body) throws Exception {
		return apply(cookie, projectId, body);
	}

	private ResultActions apply(final Cookie cookie, final long targetProjectId, final Map<String, Object> body)
		throws Exception {
		return postJson(APPLY.replace("{id}", String.valueOf(targetProjectId)), cookie, body);
	}

	private String generateToken() throws Exception {
		return readResponse(generate(pm, validGenerateBody())).get("draftToken").asString();
	}

	private static Map<String, Object> validGenerateBody() {
		return body("projectPurpose", "구매 발주를 전산화한다");
	}

	/** AI 초안의 작업 2개 중 둘 다 승인(분석 작업은 담당자·공수 수정), 마일스톤 승인, 하위작업 1개 중 1개 승인, 1개 제외. */
	private Map<String, Object> validApplyBody(final String token) {
		return body(
			"draftToken", token,
			"tasks", List.of(
				applyTask("T-001", "2026-12-01", "2026-12-10", developer.getId(), List.of(),
					List.of(body("tempId", "ST-001", "name", "요구사항 목록 정리"))),
				applyTask("T-002", "2026-12-11", "2026-12-20", null, List.of("T-001"), List.of())
			),
			"milestones", List.of(body("tempId", "M-001", "name", "설계 완료", "targetDate", "2026-12-20",
				"relatedTaskIds", List.of("T-002"))),
			"rejectedCount", 1
		);
	}

	private static Map<String, Object> applyTask(
		final String tempId,
		final String startDate,
		final String endDate,
		final Long assigneeUserId,
		final List<String> dependencies,
		final List<Map<String, Object>> subtasks
	) {
		return body(
			"tempId", tempId,
			"name", tempId + " 작업",
			"description", "설명",
			"startDate", startDate,
			"endDate", endDate,
			"estimatedPersonDays", new BigDecimal("3.5"),
			"assigneeUserId", assigneeUserId,
			"dependencies", dependencies,
			"subtasks", subtasks
		);
	}

	/** 02. AI 생성 API 명세의 응답 예시와 같은 모양의 초안. */
	private static WbsDraftProposal proposal() {
		final ProposedTask analysis = new ProposedTask("T-001", "요구사항 분석", "요구사항을 분석한다", new BigDecimal("3"),
			null, "PM", LocalDate.parse("2026-12-01"), LocalDate.parse("2026-12-10"), List.of(),
			List.of(new ProposedSubtask("ST-001", "요구사항 목록 정리", "기능 요구사항을 목록화한다", BigDecimal.ONE),
				new ProposedSubtask("ST-002", "이해관계자 인터뷰", "현업 인터뷰", BigDecimal.ONE)),
			"개발 범위 확정을 위한 선행 작업");
		final ProposedTask design = new ProposedTask("T-002", "API 및 DB 설계", "인터페이스를 설계한다", new BigDecimal("4"),
			null, "DEVELOPER", LocalDate.parse("2026-12-11"), LocalDate.parse("2026-12-20"), List.of("T-001"), List.of(),
			"구현 전에 인터페이스를 확정");
		return new WbsDraftProposal(
			List.of(new ProposedStage(WaterfallPhaseType.ANALYSIS, List.of(analysis)),
				new ProposedStage(WaterfallPhaseType.DESIGN, List.of(design))),
			List.of(new ProposedMilestone("M-001", "설계 완료", LocalDate.parse("2026-12-20"), List.of("T-002"),
				"개발 착수 전 설계 확정")),
			new DraftFeasibility("AT_RISK", "범위가 넓음"),
			List.of("팀원의 실제 가용 인일 정보가 없습니다"),
			List.of(),
			List.of(),
			List.of()
		);
	}

	private AiDraft singleAiDraft() {
		final List<AiDraft> drafts = aiDraftRepository.findAll();
		assertThat(drafts).hasSize(1);
		return drafts.get(0);
	}
}
