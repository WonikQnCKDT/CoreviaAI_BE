package org.wonik.coreviaai_be.domain.meeting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.wonik.coreviaai_be.domain.meeting.repository.MeetingRepository;
import org.wonik.coreviaai_be.domain.project.controller.ProjectApiTestSupport;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeetingApiIntegrationTest extends ProjectApiTestSupport {

	private static final String MEETINGS = PROJECTS + "/{projectId}/meetings";
	private static final String MEETING = MEETINGS + "/{meetingId}";

	private long projectId;
	private Cookie pm;
	private Cookie member;

	@org.springframework.beans.factory.annotation.Autowired
	private MeetingRepository meetingRepository;

	@BeforeEach
	void createMeetingProject() throws Exception {
		final Map<String, Object> project = validCreateBody("MEETING-2026");
		project.put("features", List.of("WBS", "KANBAN", "DELIVERABLE", "MEETING"));
		projectId = createProject(project);
		pm = loginAs(creator);
		member = loginAs(developer);
	}

	@Test
	void createsAndReadsOfficialErdShapeWithSeparateItems() throws Exception {
		final JsonNode created = createMeeting(member, meetingBody(" 주간 회의 ", "2026-11-24T01:00:00Z", "## 논의",
			List.of(body("userId", developer.getId(), "externalName", null),
				body("userId", null, "externalName", " 외부 참석자 ")),
			List.of(" 결정 A ", "결정 B"),
			List.of(body("content", " 검토 자료 제출 ", "assigneeUserId", developer.getId(),
				"dueDate", "2026-11-30"))));

		assertThat(created.get("title").asString()).isEqualTo("주간 회의");
		assertThat(created.get("heldAt").asString()).isEqualTo("2026-11-24T01:00:00Z");
		assertThat(created.get("authorUserId").asLong()).isEqualTo(developer.getId());
		assertThat(created.get("attendees")).hasSize(2);
		assertThat(created.get("attendees").get(0).get("name").asString()).isEqualTo(developer.getName());
		assertThat(created.get("attendees").get(1).get("external").asBoolean()).isTrue();
		assertThat(created.get("decisions")).hasSize(2);
		assertThat(created.get("decisions").get(0).get("sortOrder").asInt()).isZero();
		assertThat(created.get("decisions").get(0).get("source").asString()).isEqualTo("MANUAL");
		assertThat(created.get("actionItems").get(0).get("status").asString()).isEqualTo("OPEN");
		assertThat(created.get("actionItems").get(0).get("taskId").isNull()).isTrue();

		final long id = created.get("id").asLong();
		final JsonNode detail = readResponse(getAs(member, MEETING, projectId, id));
		assertThat(detail.get("content").asString()).isEqualTo("## 논의");
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM meetings", Integer.class)).isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM meeting_attendees", Integer.class)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM meeting_decisions", Integer.class)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM action_items", Integer.class)).isEqualTo(1);
	}

	@Test
	void enforcesAuthorPmMemberExecutiveAndPublicReadPermissions() throws Exception {
		final User qa = saveUser("meetingqa", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		jdbcTemplate.update("INSERT INTO project_members(project_id,user_id,actor,version,created_at,updated_at) "
			+ "VALUES (?,?,'QA',0,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)", projectId, qa.getId());
		final Cookie qaCookie = loginAs(qa);
		final long meetingId = createMeeting(member, basicMeeting("권한 회의")).get("id").asLong();

		// 전사 공개 프로젝트는 비멤버와 소속 경영진도 읽을 수 있지만 쓸 수 없다.
		readResponse(getAs(loginAs(outsider), MEETING, projectId, meetingId));
		readResponse(getAs(loginAs(itExecutive), MEETING, projectId, meetingId));
		expectError(postJson(url(MEETINGS), loginAs(outsider), basicMeeting("비멤버")),
			ErrorCode.MEETING_WRITE_FORBIDDEN);
		expectError(postJson(url(MEETINGS), loginAs(itExecutive), basicMeeting("경영진")),
			ErrorCode.MEETING_WRITE_FORBIDDEN);

		// 멤버는 생성할 수 있지만 다른 멤버의 회의록은 수정·삭제하지 못한다.
		createMeeting(qaCookie, basicMeeting("QA 작성"));
		expectError(patchJson(qaCookie, updateBody("침범", 0), MEETING, projectId, meetingId),
			ErrorCode.MEETING_WRITE_FORBIDDEN);
		expectError(deleteAs(qaCookie, MEETING, projectId, meetingId), ErrorCode.MEETING_WRITE_FORBIDDEN);

		// 작성자와 PM은 수정 가능하다.
		final JsonNode byAuthor = readResponse(patchJson(member, updateBody("작성자 수정", 0), MEETING,
			projectId, meetingId));
		assertThat(byAuthor.get("version").asLong()).isEqualTo(1);
		assertThat(readResponse(patchJson(pm, updateBody("PM 수정", 1), MEETING, projectId, meetingId))
			.get("title").asString()).isEqualTo("PM 수정");
	}

	@Test
	void searchesTitleBodyDecisionAndActionItemWithStableCursorAndEscapedWildcards() throws Exception {
		createMeeting(member, meetingBody("가장 오래된", "2026-11-20T00:00:00Z", "본문", List.of(),
			List.of(), List.of()));
		createMeeting(member, meetingBody("제목 검색", "2026-11-24T00:00:00Z", null, List.of(),
			List.of(), List.of()));
		createMeeting(member, meetingBody("둘째", "2026-11-23T00:00:00Z", "본문에 특수_검색", List.of(),
			List.of("결정 키워드"), List.of()));
		createMeeting(member, meetingBody("셋째", "2026-11-22T00:00:00Z", null, List.of(),
			List.of(), List.of(body("content", "액션 키워드", "assigneeUserId", null, "dueDate", null))));

		final JsonNode first = readResponse(getAs(member, MEETINGS + "?keyword={keyword}&size=2", projectId, "키워드"));
		assertThat(first.get("items").valueStream().map(node -> node.get("title").asString()).toList())
			.containsExactly("둘째", "셋째");
		assertThat(first.get("hasNext").asBoolean()).isFalse();

		final JsonNode page1 = readResponse(getAs(member, MEETINGS + "?size=2", projectId));
		assertThat(page1.get("items").valueStream().map(node -> node.get("title").asString()).toList())
			.containsExactly("제목 검색", "둘째");
		final String cursor = page1.get("nextCursor").asString();
		final JsonNode page2 = readResponse(getAs(member, MEETINGS + "?size=2&cursor={cursor}", projectId, cursor));
		assertThat(page2.get("items").valueStream().map(node -> node.get("title").asString()).toList())
			.containsExactly("셋째", "가장 오래된");
		expectError(getAs(member, MEETINGS + "?size=2&keyword=x&cursor={cursor}", projectId, cursor),
			ErrorCode.INVALID_CURSOR);
		assertThat(readResponse(getAs(member, MEETINGS + "?keyword={keyword}", projectId, "특수_검색"))
			.get("items")).hasSize(1);
		assertThat(readResponse(getAs(member, MEETINGS + "?keyword={keyword}", projectId, "%"))
			.get("items")).isEmpty();
	}

	@Test
	void rejectsInvalidAttendeesReferencesBoundsAndStaleVersion() throws Exception {
		final Map<String, Object> both = basicMeeting("잘못된 참석자");
		both.put("attendees", List.of(body("userId", developer.getId(), "externalName", "동시 입력")));
		expectError(postJson(url(MEETINGS), member, both), ErrorCode.INVALID_MEETING_ATTENDEE);

		final Map<String, Object> missing = basicMeeting("없는 사용자");
		missing.put("attendees", List.of(body("userId", 999_999L, "externalName", null)));
		expectError(postJson(url(MEETINGS), member, missing), ErrorCode.INVALID_MEETING_ATTENDEE);

		final Map<String, Object> duplicate = basicMeeting("중복 참석자");
		duplicate.put("attendees", List.of(body("userId", developer.getId(), "externalName", null),
			body("userId", developer.getId(), "externalName", null)));
		expectError(postJson(url(MEETINGS), member, duplicate), ErrorCode.INVALID_MEETING_ATTENDEE);

		final Map<String, Object> tooLong = basicMeeting("x".repeat(201));
		postJson(url(MEETINGS), member, tooLong).andExpect(status().isBadRequest());
		final Map<String, Object> tooMany = basicMeeting("항목 상한");
		final List<String> decisions = new ArrayList<>();
		for (int i = 0; i <= 100; i++) {
			decisions.add("결정 " + i);
		}
		tooMany.put("decisions", decisions);
		postJson(url(MEETINGS), member, tooMany).andExpect(status().isBadRequest());

		final long meetingId = createMeeting(member, basicMeeting("원본")).get("id").asLong();
		readResponse(patchJson(member, updateBody("첫 수정", 0), MEETING, projectId, meetingId));
		expectError(patchJson(member, updateBody("오래된 수정", 0), MEETING, projectId, meetingId),
			ErrorCode.CONCURRENT_MODIFICATION);
		assertThat(meetingRepository.findById(meetingId).orElseThrow().getTitle()).isEqualTo("첫 수정");
	}

	@Test
	void deletesChildrenAndRecordsOneAuditLog() throws Exception {
		final long meetingId = createMeeting(member, meetingBody("삭제 회의", "2026-11-24T00:00:00Z", "본문",
			List.of(body("userId", null, "externalName", "외부")), List.of("결정"),
			List.of(body("content", "액션", "assigneeUserId", null, "dueDate", null))))
			.get("id").asLong();

		deleteAs(member, MEETING, projectId, meetingId).andExpect(status().isNoContent());
		assertThat(meetingRepository.existsById(meetingId)).isFalse();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM meeting_attendees", Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM meeting_decisions", Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM action_items", Integer.class)).isZero();
		expectError(getAs(member, MEETING, projectId, meetingId), ErrorCode.MEETING_NOT_FOUND);

		assertThat(auditLogs(AuditAction.MEETING_DELETED)).singleElement().satisfies(log -> {
			assertThat(log.getTargetType()).isEqualTo(AuditTargetType.MEETING);
			assertThat(log.getTargetId()).isEqualTo(meetingId);
			assertThat(log.getActorUserId()).isEqualTo(developer.getId());
			assertThat(log.getDetails()).contains("삭제 회의");
		});
	}

	@Test
	void childOnlyChangesAdvanceVersionAndProtectedItemsCannotBeLost() throws Exception {
		final long id = createMeeting(member, basicMeeting("원본")).get("id").asLong();
		final Map<String, Object> update = updateBody("원본", 0);
		update.put("decisions", List.of("새 결정"));
		final JsonNode changed = readResponse(patchJson(member, update, MEETING, projectId, id));
		assertThat(changed.get("version").asLong()).isEqualTo(1);
		expectError(patchJson(member, update, MEETING, projectId, id), ErrorCode.CONCURRENT_MODIFICATION);
		jdbcTemplate.update("UPDATE meeting_decisions SET source = 'AI' WHERE meeting_id = ?", id);
		expectError(patchJson(member, updateBody("변경", 1), MEETING, projectId, id),
			ErrorCode.MEETING_ITEMS_PROTECTED);
		expectError(deleteAs(member, MEETING, projectId, id), ErrorCode.MEETING_ITEMS_PROTECTED);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM meeting_decisions WHERE meeting_id = ?",
			Integer.class, id)).isEqualTo(1);
	}

	@Test
	void retriesReturnSameMeetingRejectDifferentInputAndDoNotResurrectDeletedMeeting() throws Exception {
		final Map<String, Object> request = basicMeeting("재전송");
		final JsonNode first = createMeeting(member, request);
		final long id = first.get("id").asLong();
		assertThat(createMeeting(member, request).get("id").asLong()).isEqualTo(id);
		final Map<String, Object> changed = basicMeeting("다른 내용");
		changed.put("requestId", request.get("requestId"));
		expectError(postJson(url(MEETINGS), member, changed), ErrorCode.MEETING_REQUEST_CONFLICT);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM meetings", Integer.class)).isEqualTo(1);
		deleteAs(member, MEETING, projectId, id).andExpect(status().isNoContent());
		expectError(postJson(url(MEETINGS), member, request), ErrorCode.MEETING_REQUEST_DELETED);
		assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM meetings", Integer.class)).isZero();
	}

	@Test
	void replayReturnsCurrentMeetingAndKeyIsScopedToAuthor() throws Exception {
		final Map<String, Object> request = basicMeeting("원래 제목");
		final long firstId = createMeeting(member, request).get("id").asLong();
		readResponse(patchJson(member, updateBody("수정된 제목", 0), MEETING, projectId, firstId));
		final JsonNode replay = createMeeting(member, request);
		assertThat(replay.get("id").asLong()).isEqualTo(firstId);
		assertThat(replay.get("title").asString()).isEqualTo("수정된 제목");
		assertThat(createMeeting(pm, request).get("id").asLong()).isNotEqualTo(firstId);
	}

	@Test
	void simultaneousIdenticalRequestsCreateOneMeeting() throws Exception {
		final Map<String, Object> request = basicMeeting("동시 생성");
		final CountDownLatch start = new CountDownLatch(1);
		final var executor = Executors.newFixedThreadPool(2);
		try {
			final var first = executor.submit(() -> { start.await(); return createMeeting(member, request); });
			final var second = executor.submit(() -> { start.await(); return createMeeting(member, request); });
			start.countDown();
			assertThat(first.get(30, TimeUnit.SECONDS).get("id").asLong())
				.isEqualTo(second.get(30, TimeUnit.SECONDS).get("id").asLong());
			assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM meetings", Integer.class)).isEqualTo(1);
		} finally {
			executor.shutdownNow();
		}
	}

	@Test
	void missingOrInvalidRequestIdIsRejected() throws Exception {
		final Map<String, Object> request = basicMeeting("키 확인");
		request.remove("requestId");
		postJson(url(MEETINGS), member, request).andExpect(status().isBadRequest());
		request.put("requestId", "not-a-uuid");
		postJson(url(MEETINGS), member, request).andExpect(status().isBadRequest());
	}

	@Test
	void disabledMeetingModuleReturnsFeatureNotFoundForReadAndWrite() throws Exception {
		final Map<String, Object> project = validCreateBody("NO-MEETING");
		project.put("features", List.of("WBS", "KANBAN", "DELIVERABLE"));
		final long disabledProject = createProject(project);
		expectError(getAs(pm, MEETINGS, disabledProject), ErrorCode.FEATURE_DISABLED);
		expectError(postJson(MEETINGS.replace("{projectId}", String.valueOf(disabledProject)), pm,
			basicMeeting("비활성")), ErrorCode.FEATURE_DISABLED);
	}

	private JsonNode createMeeting(final Cookie cookie, final Map<String, Object> request) throws Exception {
		final MvcResult result = postJson(url(MEETINGS), cookie, request).andExpect(status().isCreated()).andReturn();
		return jsonMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("response");
	}

	private String url(final String template) {
		return template.replace("{projectId}", String.valueOf(projectId));
	}

	private static Map<String, Object> basicMeeting(final String title) {
		return meetingBody(title, "2026-11-24T00:00:00Z", null, List.of(), List.of(), List.of());
	}

	private static Map<String, Object> updateBody(final String title, final long version) {
		final Map<String, Object> body = basicMeeting(title);
		body.remove("requestId");
		body.put("version", version);
		return body;
	}

	private static Map<String, Object> meetingBody(
		final String title,
		final String heldAt,
		final String content,
		final List<Map<String, Object>> attendees,
		final List<String> decisions,
		final List<Map<String, Object>> actionItems
	) {
		return body("requestId", UUID.randomUUID().toString(), "title", title, "heldAt", heldAt, "content", content, "attendees", attendees,
			"decisions", decisions, "actionItems", actionItems);
	}
}
