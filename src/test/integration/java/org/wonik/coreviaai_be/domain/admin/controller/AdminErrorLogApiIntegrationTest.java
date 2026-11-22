package org.wonik.coreviaai_be.domain.admin.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.errorlog.ErrorLog;
import org.wonik.global.errorlog.ErrorLogRepository;
import org.wonik.support.ErrorTriggerTestController;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * FR-ADM-09 에러 로그: 미처리 예외의 기록(마스킹, 요청 정보, 트랜잭션 분리), 사용자 응답의 내부 정보 미노출,
 * 업무 예외 미기록, 시스템 관리자 전용 조회(기간·유형·경로 필터, 커서, 상세)를 HTTP 경계에서 검증한다.
 * 예외는 테스트 전용 컨트롤러(ErrorTriggerTestController)로 일으킨다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminErrorLogApiIntegrationTest extends AdminApiTestSupport {

	private static final String ERROR_LOGS = "/api/admin/error-logs";
	private static final String ERROR_LOG = ERROR_LOGS + "/{errorLogId}";
	private static final String TRIGGER = ErrorTriggerTestController.PATH;

	@Autowired
	private ErrorLogRepository errorLogRepository;

	@Test
	@DisplayName("일반 사용자·경영진이 에러 로그 API를 호출하면 404, 로그인하지 않으면 401")
	void onlySystemAdmin() throws Exception {
		final ErrorLog saved = saveLog(Instant.parse("2026-11-01T00:00:00Z"), "java.lang.IllegalStateException", "/api/x");
		final Department team = saveDepartment("개발팀", null);
		saveUser("user01", USER_PASSWORD, SystemRole.USER, team.getId());
		saveUser("exec01", USER_PASSWORD, SystemRole.EXECUTIVE, team.getId());

		for (final Cookie cookie : List.of(loginOk("user01", USER_PASSWORD), loginOk("exec01", USER_PASSWORD))) {
			expectError(getAs(cookie, ERROR_LOGS), ErrorCode.NOT_FOUND);
			expectError(getAs(cookie, ERROR_LOG, saved.getId()), ErrorCode.NOT_FOUND);
		}
		expectError(getAs(null, ERROR_LOGS), ErrorCode.UNAUTHORIZED);
		expectError(getAs(null, ERROR_LOG, saved.getId()), ErrorCode.UNAUTHORIZED);
	}

	@Test
	@DisplayName("미처리 예외는 공통 500만 응답하고(메시지·스택 미노출), 요청 정보와 마스킹한 메시지·원인 스택을 에러 로그로 남긴다")
	void unhandledExceptionIsRecorded() throws Exception {
		final Department team = saveDepartment("개발팀", null);
		final Long userId = saveUser("user01", USER_PASSWORD, SystemRole.USER, team.getId()).getId();
		final Cookie cookie = loginOk("user01", USER_PASSWORD);

		final ResultActions actions = mockMvc.perform(get(TRIGGER + "/unhandled")
			.queryParam("apiKey", "q-secret-1")
			.header(HttpHeaders.AUTHORIZATION, "Bearer header-secret-2")
			.cookie(cookie));
		expectError(actions, ErrorCode.INTERNAL_SERVER_ERROR);
		final String body = actions.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		assertThat(body).doesNotContain("Secr3t", "IllegalState", "Exception", "\tat ", "hong", "Caused");

		final List<ErrorLog> logs = errorLogRepository.findAll();
		assertThat(logs).singleElement().satisfies(log -> {
			assertThat(log.getOccurredAt()).isEqualTo(clock.instant());
			assertThat(log.getHttpMethod()).isEqualTo("GET");
			assertThat(log.getApiPath()).isEqualTo(TRIGGER + "/unhandled");
			assertThat(log.getUserId()).isEqualTo(userId);
			assertThat(log.getErrorType()).isEqualTo(IllegalStateException.class.getName());
			assertThat(log.getMessage()).isEqualTo("db login failed password=**** contact ****");
			assertThat(log.getStackTrace())
				.startsWith(IllegalStateException.class.getName() + ": db login failed password=**** contact ****\n\tat ")
				.contains("Caused by: java.lang.RuntimeException: Key (login_id)=(****) already exists.")
				.doesNotContain("Secr3t", "hong", "q-secret-1", "header-secret-2", cookie.getValue());
		});
	}

	@Test
	@DisplayName("컨트롤러가 던진 checked 예외도 미처리 예외로 500을 주고 기록한다")
	void checkedExceptionIsRecorded() throws Exception {
		saveUser("user01", USER_PASSWORD, SystemRole.USER, saveDepartment("개발팀", null).getId());

		expectError(getAs(loginOk("user01", USER_PASSWORD), TRIGGER + "/checked"), ErrorCode.INTERNAL_SERVER_ERROR);

		assertThat(errorLogRepository.findAll()).singleElement().satisfies(log -> {
			assertThat(log.getErrorType()).isEqualTo("java.io.IOException");
			assertThat(log.getMessage()).isEqualTo("disk full token=****");
		});
	}

	@Test
	@DisplayName("업무 트랜잭션이 롤백돼도 에러 로그는 별도 트랜잭션으로 남는다")
	void recordSurvivesRollback() throws Exception {
		saveUser("user01", USER_PASSWORD, SystemRole.USER, saveDepartment("개발팀", null).getId());

		expectError(postJson(TRIGGER + "/rollback", loginOk("user01", USER_PASSWORD), body()), ErrorCode.INTERNAL_SERVER_ERROR);

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM code_groups WHERE group_code = ?", Integer.class,
			ErrorTriggerTestController.ROLLBACK_GROUP_CODE)).isZero();
		assertThat(errorLogRepository.findAll()).singleElement()
			.satisfies(log -> assertThat(log.getHttpMethod()).isEqualTo("POST"));
	}

	@Test
	@DisplayName("업무 예외·입력 오류·권한 거부·매핑 없는 경로·스프링 표준 4xx는 원래 응답을 주고 에러 로그에 남기지 않는다")
	void intendedErrorsAreNotRecorded() throws Exception {
		saveUser("user01", USER_PASSWORD, SystemRole.USER, saveDepartment("개발팀", null).getId());
		final Cookie userCookie = loginOk("user01", USER_PASSWORD);

		expectError(getAs(userCookie, TRIGGER + "/business"), ErrorCode.PROJECT_NOT_FOUND);
		expectError(getAs(userCookie, ERROR_LOGS), ErrorCode.NOT_FOUND);
		getAs(adminCookie, ERROR_LOGS + "?size=0").andExpect(status().isBadRequest());
		getAs(adminCookie, ERROR_LOGS + "?from=yesterday").andExpect(status().isBadRequest());
		// 매핑 없는 경로는 본문 없는 404가 아니라 다른 404와 같은 공통 본문을 준다.
		expectError(getAs(userCookie, "/api/no-such-path"), ErrorCode.NOT_FOUND);
		// 스프링 표준 4xx(지원하지 않는 메서드)도 기록하지 않는다.
		expectError(postJson(ERROR_LOGS, adminCookie, body()), ErrorCode.NOT_FOUND);
		// 처리기가 다시 던져 스프링 기본 처리에 맡기는 4xx(지원하지 않는 Content-Type)도 원래 상태 코드 그대로다.
		mockMvc.perform(post("/api/admin/i18n/labels").cookie(adminCookie).contentType(MediaType.TEXT_PLAIN).content("x"))
			.andExpect(status().isUnsupportedMediaType());

		assertThat(errorLogRepository.count()).isZero();
	}

	@Test
	@DisplayName("목록은 최신순·커서 페이지이고 스택 트레이스를 넣지 않으며, 상세에서만 스택을 준다")
	void listAndDetail() throws Exception {
		final List<Long> ids = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			ids.add(saveLog(Instant.parse("2026-11-01T00:00:00Z").plusSeconds(i), "java.lang.IllegalStateException",
				"/api/x/" + i).getId());
		}

		final JsonNode first = readResponse(getAs(adminCookie, ERROR_LOGS + "?size=2"));
		assertThat(idsOf(first)).containsExactly(ids.get(4), ids.get(3));
		assertThat(first.get("hasNext").asBoolean()).isTrue();
		final JsonNode item = first.get("items").get(0);
		assertThat(item.has("stackTrace")).isFalse();
		assertThat(item.get("apiPath").asString()).isEqualTo("/api/x/4");
		assertThat(item.get("errorType").asString()).isEqualTo("java.lang.IllegalStateException");
		assertThat(item.get("message").asString()).isEqualTo("boom password=****");
		assertThat(item.get("occurredAt").asString()).isEqualTo("2026-11-01T00:00:04Z");

		final JsonNode second = readResponse(getAs(adminCookie,
			ERROR_LOGS + "?size=2&cursor=" + first.get("nextCursor").asString()));
		assertThat(idsOf(second)).containsExactly(ids.get(2), ids.get(1));
		final JsonNode last = readResponse(getAs(adminCookie,
			ERROR_LOGS + "?size=2&cursor=" + second.get("nextCursor").asString()));
		assertThat(idsOf(last)).containsExactly(ids.get(0));
		assertThat(last.get("hasNext").asBoolean()).isFalse();
		assertThat(last.get("nextCursor").isNull()).isTrue();

		final JsonNode detail = readResponse(getAs(adminCookie, ERROR_LOG, ids.get(0)));
		assertThat(detail.get("id").asLong()).isEqualTo(ids.get(0));
		assertThat(detail.get("stackTrace").asString()).startsWith("java.lang.IllegalStateException: boom password=****\n\tat ");

		expectError(getAs(adminCookie, ERROR_LOG, ids.get(4) + 1000), ErrorCode.ERROR_LOG_NOT_FOUND);
		getAs(adminCookie, ERROR_LOG, 0).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("기간(from 포함·to 제외)·에러 유형(정확히 일치)·API 경로(앞부분 일치) 조건으로 조회한다")
	void filters() throws Exception {
		final Long early = saveLog(Instant.parse("2026-10-31T23:59:59Z"), "java.lang.IllegalStateException", "/api/projects/1").getId();
		final Long start = saveLog(Instant.parse("2026-11-01T00:00:00Z"), "java.lang.NullPointerException", "/api/projects/2").getId();
		final Long middle = saveLog(Instant.parse("2026-11-15T00:00:00Z"), "java.lang.IllegalStateException", "/api/admin_x/1").getId();
		final Long end = saveLog(Instant.parse("2026-12-01T00:00:00Z"), "java.lang.IllegalStateException", "/api/projects/3").getId();

		assertThat(idsOf(readResponse(getAs(adminCookie,
			ERROR_LOGS + "?from=2026-11-01T00:00:00Z&to=2026-12-01T00:00:00Z")))).containsExactly(middle, start);
		assertThat(idsOf(readResponse(getAs(adminCookie, ERROR_LOGS + "?from=2026-11-01T00:00:00Z"))))
			.containsExactly(end, middle, start);
		assertThat(idsOf(readResponse(getAs(adminCookie, ERROR_LOGS + "?to=2026-11-01T00:00:00Z")))).containsExactly(early);
		assertThat(idsOf(readResponse(getAs(adminCookie, ERROR_LOGS + "?errorType=java.lang.NullPointerException"))))
			.containsExactly(start);
		assertThat(idsOf(readResponse(getAs(adminCookie, ERROR_LOGS + "?errorType=IllegalStateException")))).isEmpty();
		assertThat(idsOf(readResponse(getAs(adminCookie, ERROR_LOGS + "?apiPath=/api/projects")))).containsExactly(end, start, early);
		// 밑줄은 LIKE 와일드카드가 아니라 글자로 찾는다: /api/admin_x만 일치하고 /api/adminYx 같은 경로는 일치하지 않는다.
		saveLog(Instant.parse("2026-11-16T00:00:00Z"), "java.lang.IllegalStateException", "/api/adminYx/1");
		assertThat(idsOf(readResponse(getAs(adminCookie, ERROR_LOGS + "?apiPath=/api/admin_x")))).containsExactly(middle);
		assertThat(idsOf(readResponse(getAs(adminCookie, ERROR_LOGS
			+ "?from=2026-11-01T00:00:00Z&errorType=java.lang.IllegalStateException&apiPath=/api/projects"))))
			.containsExactly(end);
		assertThat(idsOf(readResponse(getAs(adminCookie, ERROR_LOGS)))).hasSize(5);

		expectError(getAs(adminCookie, ERROR_LOGS + "?from=2026-11-02T00:00:00Z&to=2026-11-01T00:00:00Z"), ErrorCode.INVALID_PERIOD);
		expectError(getAs(adminCookie, ERROR_LOGS + "?from=2026-11-01T00:00:00Z&to=2026-11-01T00:00:00Z"), ErrorCode.INVALID_PERIOD);
		getAs(adminCookie, ERROR_LOGS + "?apiPath=" + "a".repeat(301)).andExpect(status().isBadRequest());
	}

	private ErrorLog saveLog(final Instant occurredAt, final String errorType, final String apiPath) {
		final Exception error = IllegalStateException.class.getName().equals(errorType)
			? new IllegalStateException("boom password=hunter2")
			: new NullPointerException("boom password=hunter2");
		return errorLogRepository.save(ErrorLog.of(occurredAt, "GET", apiPath, null, error));
	}

	private static List<Long> idsOf(final JsonNode page) {
		final List<Long> ids = new ArrayList<>();
		page.get("items").forEach(item -> ids.add(item.get("id").asLong()));
		return ids;
	}
}
