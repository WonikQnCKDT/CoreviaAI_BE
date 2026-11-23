package org.wonik.coreviaai_be.domain.schedule.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.project.controller.ProjectApiTestSupport;
import org.wonik.global.common.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PersonalScheduleApiIntegrationTest extends ProjectApiTestSupport {

	private static final String URL = "/api/me/events";
	private static final String RANGE_FROM = "2026-11-09T00:00:00Z";
	private static final String RANGE_TO = "2026-11-10T00:00:00Z";

	@Test
	void createsAndListsOnlyEventsOverlappingTheRange() throws Exception {
		final Cookie cookie = loginAs(developer);
		final JsonNode created = created(postJson(URL, cookie, body(
			"title", "  Sprint review  ",
			"startAt", "2026-11-09T01:00:00Z",
			"endAt", "2026-11-09T02:00:00Z",
			"memo", "room 3")));
		assertThat(created.get("title").asString()).isEqualTo("Sprint review");
		assertThat(created.get("allDay").asBoolean()).isFalse();

		assertThat(list(cookie, RANGE_FROM, RANGE_TO)).hasSize(1);
		// 끝이 기간 시작과 같은 일정은 겹치지 않는다. 다음 날 기간에도 나오지 않는다.
		assertThat(list(cookie, "2026-11-08T00:00:00Z", RANGE_FROM)).isEmpty();
		assertThat(list(cookie, RANGE_TO, "2026-11-11T00:00:00Z")).isEmpty();
	}

	@Test
	void patchOverwritesOwnEventAndHidesItFromOtherUsers() throws Exception {
		final Cookie owner = loginAs(developer);
		final long id = created(postJson(URL, owner, body(
			"title", "Old", "startAt", "2026-11-09T01:00:00Z", "endAt", "2026-11-09T02:00:00Z")))
			.get("id").asLong();

		final JsonNode updated = readResponse(patchJson(owner, body(
			"title", "New", "startAt", "2026-11-09T03:00:00Z", "endAt", "2026-11-09T04:00:00Z", "allDay", true),
			URL + "/{eventId}", id));
		assertThat(updated.get("title").asString()).isEqualTo("New");
		assertThat(updated.get("allDay").asBoolean()).isTrue();
		assertThat(updated.get("memo").isNull()).isTrue();

		final Cookie other = loginAs(outsider);
		expectError(patchJson(other, body("title", "Hijack", "startAt", "2026-11-09T03:00:00Z",
			"endAt", "2026-11-09T04:00:00Z"), URL + "/{eventId}", id), ErrorCode.SCHEDULE_NOT_FOUND);
		assertThat(list(other, RANGE_FROM, RANGE_TO)).isEmpty();
	}

	@Test
	void rejectsZeroOrReversedPeriod() throws Exception {
		final Cookie cookie = loginAs(developer);
		expectError(postJson(URL, cookie, body("title", "Same", "startAt", "2026-11-09T01:00:00Z",
			"endAt", "2026-11-09T01:00:00Z")), ErrorCode.INVALID_SCHEDULE_PERIOD);
		expectError(postJson(URL, cookie, body("title", "Reversed", "startAt", "2026-11-09T02:00:00Z",
			"endAt", "2026-11-09T01:00:00Z")), ErrorCode.INVALID_SCHEDULE_PERIOD);
	}

	@Test
	void rejectsQueryRangeThatIsReversedOrTooLong() throws Exception {
		final Cookie cookie = loginAs(developer);
		expectError(getAs(cookie, URL + "?from={from}&to={to}", RANGE_TO, RANGE_FROM), ErrorCode.INVALID_QUERY_RANGE);
		expectError(getAs(cookie, URL + "?from={from}&to={to}", "2026-01-01T00:00:00Z", "2027-01-03T00:00:00Z"),
			ErrorCode.INVALID_QUERY_RANGE);
	}

	@Test
	void requiresLogin() throws Exception {
		expectError(getAs(null, URL + "?from={from}&to={to}", RANGE_FROM, RANGE_TO), ErrorCode.UNAUTHORIZED);
	}

	/** 201 Created 응답의 response 본문. readResponse는 200만 받으므로 상태 검증 없이 읽는다. */
	private JsonNode created(final ResultActions actions) throws Exception {
		final MvcResult result = actions.andExpect(status().isCreated()).andReturn();
		return jsonMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("response");
	}

	private List<JsonNode> list(final Cookie cookie, final String from, final String to) throws Exception {
		final var result = getAs(cookie, URL + "?from={from}&to={to}", from, to).andReturn();
		return jsonMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
			.get("response").valueStream().toList();
	}
}
