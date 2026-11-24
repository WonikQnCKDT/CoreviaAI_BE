package org.wonik.coreviaai_be.domain.i18n.controller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.project.controller.ProjectApiTestSupport;
import org.wonik.global.common.exception.ErrorCode;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LanguagePreferenceApiIntegrationTest extends ProjectApiTestSupport {
    @Test void savesOnlyOwnPreferenceAndUsesItAfterNewLogin() throws Exception {
        var cookie = loginAs(developer);
        var current = readResponse(getAs(cookie, "/api/me/language"));
        var request = body("language", "zh-TW", "version", current.get("version").asLong());
        var updated = readResponse(putJson(cookie, request, "/api/me/language"));
        assertThat(updated.get("effectiveLanguage").asString()).isEqualTo("zh-TW");
        assertThat(readResponse(putJson(cookie, request, "/api/me/language")).get("version")).isEqualTo(updated.get("version"));
        assertThat(readResponse(getAs(loginAs(developer), "/api/me/language/labels")).get("lang").asString()).isEqualTo("zh-TW");
        assertThat(readResponse(getAs(loginAs(outsider), "/api/me/language")).get("preferredLanguage").isNull()).isTrue();
        expectError(putJson(cookie, body("language", "en", "version", 999), "/api/me/language"), ErrorCode.CONCURRENT_MODIFICATION);
        putJson(cookie, body("language", "ja", "version", updated.get("version").asLong()), "/api/me/language")
            .andExpect(status().isBadRequest());
        var cleared = readResponse(putJson(cookie, body("language", null, "version", updated.get("version").asLong()), "/api/me/language"));
        assertThat(cleared.get("preferredLanguage").isNull()).isTrue();
        assertThat(cleared.get("effectiveLanguage").asString()).isEqualTo("ko");
        assertThat(jdbcTemplate.queryForObject("select count(*) from audit_logs where action = 'USER_LANGUAGE_CHANGED'", Integer.class)).isEqualTo(2);
    }
}
