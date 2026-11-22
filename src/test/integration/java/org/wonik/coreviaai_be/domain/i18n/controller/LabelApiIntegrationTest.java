package org.wonik.coreviaai_be.domain.i18n.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import org.wonik.authserver.domain.credential.entity.LocalCredential;
import org.wonik.coreviaai_be.domain.i18n.entity.I18nLabel;
import org.wonik.coreviaai_be.domain.i18n.repository.I18nLabelRepository;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.support.ApiIntegrationTestSupport;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * FR-ADM-06 화면용 라벨 묶음 API를 HTTP 경계에서 검증한다.
 * 로그인 없이 호출(로그인 화면), 비밀번호 변경 강제 세션 허용, 언어 결정 순서, 번역 누락 한국어 표시, 재배포 없는 반영.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LabelApiIntegrationTest extends ApiIntegrationTestSupport {

	private static final String BUNDLE = "/api/i18n/labels";

	@Autowired
	private I18nLabelRepository i18nLabelRepository;

	@Test
	@DisplayName("로그인하지 않아도 조회할 수 있고, 언어를 정하지 않으면 한국어 시드 문구를 준다")
	void publicKoreanDefault() throws Exception {
		final ResultActions actions = mockMvc.perform(get(BUNDLE))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.VARY, HttpHeaders.ACCEPT_LANGUAGE));
		final JsonNode bundle = readResponse(actions);
		assertThat(bundle.get("lang").asString()).isEqualTo("ko");
		assertThat(bundle.get("labels").get("code.TASK_STATUS.IN_PROGRESS").asString()).isEqualTo("진행");
		assertThat(bundle.get("labels").get("codeGroup.RISK_LEVEL").asString()).isEqualTo("위험 수준");
	}

	@Test
	@DisplayName("lang 파라미터가 Accept-Language보다 우선하고, 둘 다 지원하지 않으면 한국어")
	void languageResolution() throws Exception {
		assertThat(bundle("en", null).get("labels").get("code.TASK_STATUS.DONE").asString()).isEqualTo("Done");
		assertThat(bundle(null, "zh-TW,zh;q=0.9").get("labels").get("code.TASK_STATUS.DONE").asString()).isEqualTo("已完成");
		final JsonNode paramWins = bundle("en", "zh-TW");
		assertThat(paramWins.get("lang").asString()).isEqualTo("en");
		final JsonNode headerFallback = bundle("ja", "en-US,en;q=0.9");
		assertThat(headerFallback.get("lang").asString()).isEqualTo("en");
		final JsonNode unsupported = bundle("zh-CN", "fr-FR");
		assertThat(unsupported.get("lang").asString()).isEqualTo("ko");
		assertThat(bundle(null, "en;q=abc").get("lang").asString()).isEqualTo("ko");
	}

	@Test
	@DisplayName("번역 누락 라벨은 한국어로 표시하고, 관리자가 바꾼 문구는 재배포·캐시 무효화 없이 다음 조회에 반영된다")
	void missingFallsBackAndChangesApplyImmediately() throws Exception {
		final I18nLabel label = i18nLabelRepository.save(I18nLabel.create("test.menu.home", "홈", null, null));
		assertThat(bundle("en", null).get("labels").get("test.menu.home").asString()).isEqualTo("홈");
		assertThat(bundle("zh-TW", null).get("labels").get("test.menu.home").asString()).isEqualTo("홈");

		label.changeTexts("홈", "Home", null);
		i18nLabelRepository.save(label);
		assertThat(bundle("en", null).get("labels").get("test.menu.home").asString()).isEqualTo("Home");
		assertThat(bundle("zh-TW", null).get("labels").get("test.menu.home").asString()).isEqualTo("홈");
	}

	@Test
	@DisplayName("로그인 사용자·시스템 관리자도 조회하고, 비밀번호 변경이 강제된 세션도 문구는 받는다(다른 업무 API는 403)")
	void authenticatedAndPasswordChangeSession() throws Exception {
		saveUser("admin01", USER_PASSWORD, SystemRole.SYSTEM_ADMIN, null);
		final Cookie admin = loginOk("admin01", USER_PASSWORD);
		getAs(admin, BUNDLE).andExpect(status().isOk());

		final User temp = userRepository.save(User.create("temp01", "임시 사용자", SystemRole.SYSTEM_ADMIN, null, null));
		credentialRepository.save(LocalCredential.issue(temp.getId(), passwordEncoder.encode(USER_PASSWORD), clock.instant()));
		final Cookie tempCookie = loginOk("temp01", USER_PASSWORD);
		getAs(tempCookie, BUNDLE + "?lang=en").andExpect(status().isOk());
		expectError(getAs(tempCookie, "/api/codes/TASK_STATUS"), ErrorCode.PASSWORD_CHANGE_REQUIRED);
		expectError(getAs(tempCookie, "/api/admin/i18n/labels"), ErrorCode.PASSWORD_CHANGE_REQUIRED);
	}

	@Test
	@DisplayName("만료·위조 토큰 쿠키가 있어도 공개 조회는 200, 공개는 GET 조회 하나뿐이다")
	void invalidCookieAndOtherMethods() throws Exception {
		getAs(new Cookie("AccessToken", "invalid.token.value"), BUNDLE).andExpect(status().isOk());
		expectError(postJson(BUNDLE, null, body("labelKey", "test.x", "textKo", "엑스")), ErrorCode.UNAUTHORIZED);
		expectError(getAs(null, "/api/admin/i18n/labels"), ErrorCode.UNAUTHORIZED);
	}

	private JsonNode bundle(final String lang, final String acceptLanguage) throws Exception {
		var request = get(BUNDLE);
		if (lang != null) {
			request = request.param("lang", lang);
		}
		if (acceptLanguage != null) {
			request = request.header(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage);
		}
		return readResponse(mockMvc.perform(request));
	}
}
