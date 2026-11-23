package org.wonik.coreviaai_be.domain.worklog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
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
class WorklogApiIntegrationTest extends ProjectApiTestSupport {

	private static final String URL = "/api/me/worklogs";

	@Test
	void putCreatesThenReplacesTheSameDayWithoutDuplicates() throws Exception {
		final Cookie cookie = loginAs(developer);
		final JsonNode first = readResponse(putJson(cookie, body("content", "## 오늘\n- 칸반 검수"),
			URL + "/{logDate}", "2026-11-09"));
		assertThat(first.get("logDate").asString()).isEqualTo("2026-11-09");
		assertThat(first.get("content").asString()).isEqualTo("## 오늘\n- 칸반 검수");

		final JsonNode second = readResponse(putJson(cookie, body("content", "수정본"),
			URL + "/{logDate}", "2026-11-09"));
		assertThat(second.get("content").asString()).isEqualTo("수정본");

		assertThat(readResponse(getAs(cookie, URL + "/{logDate}", "2026-11-09")).get("content").asString())
			.isEqualTo("수정본");
		assertThat(jdbcTemplate.queryForObject(
			"select count(*) from worklogs where user_id = ? and log_date = ?", Integer.class,
			developer.getId(), Date.valueOf("2026-11-09"))).isEqualTo(1);
	}

	@Test
	void getMissingDayIsNotFoundAndOtherUsersCannotSeeIt() throws Exception {
		final Cookie owner = loginAs(developer);
		readResponse(putJson(owner, body("content", "private"), URL + "/{logDate}", "2026-11-10"));

		expectError(getAs(owner, URL + "/{logDate}", "2026-11-11"), ErrorCode.WORKLOG_NOT_FOUND);
		expectError(getAs(loginAs(outsider), URL + "/{logDate}", "2026-11-10"), ErrorCode.WORKLOG_NOT_FOUND);
	}

	@Test
	void listsRangeInDateOrderIncludingBothEnds() throws Exception {
		final Cookie cookie = loginAs(developer);
		putJson(cookie, body("content", "c"), URL + "/{logDate}", "2026-11-12").andExpect(status().isOk());
		putJson(cookie, body("content", "a"), URL + "/{logDate}", "2026-11-10").andExpect(status().isOk());
		putJson(cookie, body("content", "b"), URL + "/{logDate}", "2026-11-11").andExpect(status().isOk());
		putJson(cookie, body("content", "out"), URL + "/{logDate}", "2026-11-13").andExpect(status().isOk());

		final List<JsonNode> items = jsonMapper.readTree(getAs(cookie, URL + "?from={from}&to={to}",
			"2026-11-10", "2026-11-12").andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8))
			.get("response").valueStream().toList();
		assertThat(items).extracting(item -> item.get("content").asString()).containsExactly("a", "b", "c");
	}

	@Test
	void rejectsReversedOrOverlongRangeAndOversizedContent() throws Exception {
		final Cookie cookie = loginAs(developer);
		expectError(getAs(cookie, URL + "?from={from}&to={to}", "2026-11-12", "2026-11-10"),
			ErrorCode.INVALID_QUERY_RANGE);
		expectError(getAs(cookie, URL + "?from={from}&to={to}", "2026-01-01", "2027-01-02"),
			ErrorCode.INVALID_QUERY_RANGE);
		putJson(cookie, body("content", "x".repeat(10_001)), URL + "/{logDate}", "2026-11-14")
			.andExpect(status().isBadRequest());
	}
}
