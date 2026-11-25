package org.wonik.coreviaai_be.domain.menu.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.support.ApiIntegrationTestSupport;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MenuApiIntegrationTest extends ApiIntegrationTestSupport {

	private Cookie adminCookie;
	private Cookie userCookie;

	@BeforeEach
	void arrange() throws Exception {
		final User admin = saveUser("menu-admin", USER_PASSWORD, SystemRole.SYSTEM_ADMIN, null);
		final User user = saveUser("menu-user", USER_PASSWORD, SystemRole.USER, null);
		adminCookie = loginOk(admin.getLoginId(), USER_PASSWORD);
		userCookie = loginOk(user.getLoginId(), USER_PASSWORD);
	}

	@Test
	@DisplayName("일반 사용자는 관리자 메뉴가 빠지고, 요청 언어의 문구를 받는다")
	void userSeesRoleFilteredMenusInRequestedLanguage() throws Exception {
		final JsonNode korean = readResponse(getAs(userCookie, "/api/menus?lang=ko"));
		assertThat(texts(korean, "my-work")).isEqualTo("내 업무");
		assertThat(keys(korean)).contains("projects", "knowledge", "portfolio").doesNotContain("admin");

		final JsonNode english = readResponse(getAs(userCookie, "/api/menus?lang=en"));
		assertThat(texts(english, "my-work")).isEqualTo("My work");

		final User executive = saveUser("menu-exec", USER_PASSWORD, SystemRole.EXECUTIVE, null);
		final JsonNode execMenus = readResponse(getAs(loginOk(executive.getLoginId(), USER_PASSWORD), "/api/menus"));
		assertThat(keys(execMenus)).doesNotContain("admin");
	}

	@Test
	@DisplayName("선호 언어가 있으면 요청 lang보다 우선한다")
	void preferredLanguageOverridesRequest() throws Exception {
		final User user = userRepository.findByLoginId("menu-user").orElseThrow();
		user.changePreferredLanguage("en");
		userRepository.save(user);

		final JsonNode menus = readResponse(getAs(userCookie, "/api/menus?lang=ko"));
		assertThat(texts(menus, "my-work")).isEqualTo("My work");
	}

	@Test
	@DisplayName("관리자가 문구를 바꾸면 재조회에 반영되고 감사 로그가 남는다")
	void adminUpdatesTextWithoutRedeploy() throws Exception {
		readResponse(postJson("/api/admin/i18n/labels", adminCookie,
			body("labelKey", "test.menu.sample", "textKo", "샘플", "textEn", null, "textZhTw", null)));
		jdbcTemplate.update(
			"INSERT INTO menus (menu_key, label_key, url_path, sort_order, required_role, active, created_at, updated_at) "
				+ "VALUES ('test-sample', 'test.menu.sample', '/test-sample', 99, NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

		assertThat(texts(readResponse(getAs(userCookie, "/api/menus?lang=en")), "test-sample")).isEqualTo("샘플");
		final JsonNode before = menu(readResponse(getAs(adminCookie, "/api/admin/menus")), "test-sample");
		final JsonNode updated = readResponse(patchJson(adminCookie,
			body("textKo", "바꾼 메뉴", "textEn", "Renamed", "textZhTw", null, "version", before.get("version").asLong()),
			"/api/admin/menus/{menuId}", before.get("id").asLong()));
		assertThat(updated.get("textKo").asString()).isEqualTo("바꾼 메뉴");
		assertThat(updated.get("menuKey").asString()).isEqualTo("test-sample");

		assertThat(texts(readResponse(getAs(userCookie, "/api/menus?lang=ko")), "test-sample")).isEqualTo("바꾼 메뉴");
		assertThat(texts(readResponse(getAs(userCookie, "/api/menus?lang=en")), "test-sample")).isEqualTo("Renamed");

		assertThat(auditLogs(AuditAction.I18N_LABEL_CHANGED)).anySatisfy(log -> {
			final JsonNode details = jsonMapper.readTree(log.getDetails());
			assertThat(details.get("labelKey").asString()).isEqualTo("test.menu.sample");
			assertThat(details.get("lang").asString()).isEqualTo("ko");
			assertThat(details.get("text").get("before").asString()).isEqualTo("샘플");
			assertThat(details.get("text").get("after").asString()).isEqualTo("바꾼 메뉴");
		});

		expectError(patchJson(userCookie,
			body("textKo", "침입", "version", updated.get("version").asLong()),
			"/api/admin/menus/{menuId}", before.get("id").asLong()), ErrorCode.NOT_FOUND);
		patchJson(adminCookie, body("textKo", "", "version", updated.get("version").asLong()),
			"/api/admin/menus/{menuId}", before.get("id").asLong()).andExpect(status().isBadRequest());
		expectError(patchJson(adminCookie,
			body("textKo", "또 변경", "version", before.get("version").asLong()),
			"/api/admin/menus/{menuId}", before.get("id").asLong()), ErrorCode.CONCURRENT_MODIFICATION);
		expectError(getAs(null, "/api/menus"), ErrorCode.UNAUTHORIZED);
		expectError(patchJson(adminCookie,
			body("textKo", "없는 메뉴", "version", 0),
			"/api/admin/menus/{menuId}", 999999999L), ErrorCode.MENU_NOT_FOUND);
	}

	private static JsonNode menu(final JsonNode menus, final String menuKey) {
		for (final JsonNode menu : menus) {
			if (menuKey.equals(menu.get("menuKey").asString())) {
				return menu;
			}
		}
		throw new AssertionError("메뉴 없음: " + menuKey);
	}

	private static String texts(final JsonNode menus, final String menuKey) {
		return menu(menus, menuKey).get("text").asString();
	}

	private static List<String> keys(final JsonNode menus) {
		final List<String> keys = new ArrayList<>();
		menus.forEach(menu -> keys.add(menu.get("menuKey").asString()));
		return keys;
	}
}
