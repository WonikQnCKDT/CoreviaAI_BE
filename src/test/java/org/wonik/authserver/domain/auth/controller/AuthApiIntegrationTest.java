package org.wonik.authserver.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.wonik.authserver.domain.auth.loginhistory.LoginFailureReason;
import org.wonik.authserver.domain.auth.loginhistory.LoginHistory;
import org.wonik.authserver.domain.auth.loginhistory.LoginHistoryRepository;
import org.wonik.authserver.domain.auth.loginhistory.LoginResult;
import org.wonik.authserver.domain.auth.policy.PasswordPolicy;
import org.wonik.global.jwt.AuthCookieManager;
import org.wonik.global.common.type.SystemRole;
import org.wonik.authserver.domain.credential.entity.LocalCredential;
import org.wonik.authserver.domain.credential.repository.LocalCredentialRepository;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;
import org.wonik.support.InMemoryAuthSessionStore;
import org.wonik.support.MutableClock;
import org.wonik.support.TestClockConfig;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * FR-ADM-01 인수 기준을 HTTP 경계에서 검증한다.
 * 미인증 차단, 30분 무활동 만료, 5회 연속 실패 잠금, 비밀번호 변경·관리자 초기화, 접속 로그 기록.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIntegrationTest {

	private static final String USER_ID = "user01";
	private static final String USER_PASSWORD = "Passw0rd!";
	private static final String ADMIN_ID = "admin01";
	private static final String ADMIN_PASSWORD = "Adm1n-pass!";
	private static final String NEW_PASSWORD = "newpass9#";

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private JsonMapper jsonMapper;
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private LocalCredentialRepository credentialRepository;
	@Autowired
	private LoginHistoryRepository loginHistoryRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private JdbcTemplate jdbcTemplate;
	@Autowired
	private InMemoryAuthSessionStore sessionStore;
	@Autowired
	private MutableClock clock;

	@BeforeEach
	void setUp() {
		clock.setInstant(TestClockConfig.START);
		sessionStore.clear();
		loginHistoryRepository.deleteAll();
		credentialRepository.deleteAll();
		userRepository.deleteAll();
		saveUser(USER_ID, USER_PASSWORD, SystemRole.USER);
		saveUser(ADMIN_ID, ADMIN_PASSWORD, SystemRole.SYSTEM_ADMIN);
	}

	// ---- 미인증 접근 ----

	@Test
	@DisplayName("로그인하지 않으면 API에 접근할 수 없다(401 UNAUTHORIZED)")
	void unauthenticatedRequestIsRejected() throws Exception {
		expectError(mockMvc.perform(get("/api/auth/me")), ErrorCode.UNAUTHORIZED);
	}

	@Test
	@DisplayName("위조된 AccessToken은 401 INVALID_TOKEN")
	void forgedTokenIsRejected() throws Exception {
		expectError(
			mockMvc.perform(get("/api/auth/me").cookie(new Cookie(AuthCookieManager.ACCESS_TOKEN_COOKIE, "forged.token.value"))),
			ErrorCode.INVALID_TOKEN
		);
	}

	@Test
	@DisplayName("RefreshToken을 AccessToken 쿠키에 넣어도 인증되지 않는다")
	void refreshTokenCannotBeUsedAsAccessToken() throws Exception {
		final Tokens tokens = loginOk(USER_ID, USER_PASSWORD);

		expectError(
			mockMvc.perform(get("/api/auth/me").cookie(new Cookie(AuthCookieManager.ACCESS_TOKEN_COOKIE, tokens.refresh()))),
			ErrorCode.INVALID_TOKEN
		);
	}

	// ---- 모듈별 보안 체인 ----

	@Test
	@DisplayName("업무 API(coreviaai_be 체인)도 로그인하지 않으면 401")
	void businessApiRequiresLogin() throws Exception {
		expectError(mockMvc.perform(get("/api/projects")), ErrorCode.UNAUTHORIZED);
	}

	@Test
	@DisplayName("로그인하면 업무 체인의 인증을 통과한다(엔드포인트가 아직 없어 404)")
	void businessApiAcceptsLoggedInUser() throws Exception {
		final Tokens tokens = loginOk(USER_ID, USER_PASSWORD);

		mockMvc.perform(get("/api/projects").cookie(tokens.accessCookie())).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("업무 체인의 /api/admin/**은 시스템 관리자만 통과한다")
	void businessAdminApiRequiresAdmin() throws Exception {
		final Tokens user = loginOk(USER_ID, USER_PASSWORD);
		final Tokens admin = loginOk(ADMIN_ID, ADMIN_PASSWORD);

		expectError(mockMvc.perform(get("/api/admin/organizations").cookie(user.accessCookie())), ErrorCode.NOT_FOUND);
		mockMvc.perform(get("/api/admin/organizations").cookie(admin.accessCookie())).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("어느 모듈 체인에도 속하지 않는 경로는 기본 체인이 막는다")
	void unmatchedPathIsDenied() throws Exception {
		final Tokens tokens = loginOk(USER_ID, USER_PASSWORD);

		expectError(mockMvc.perform(get("/internal/anything")), ErrorCode.UNAUTHORIZED);
		mockMvc.perform(get("/internal/anything").cookie(tokens.accessCookie())).andExpect(status().is4xxClientError());
	}

	// ---- 로그인 ----

	@Test
	@DisplayName("로그인 성공: HttpOnly·SameSite 쿠키 발급, 계정 정보 응답, 성공 접속 로그 기록")
	void loginSucceeds() throws Exception {
		final MvcResult result = login(USER_ID, USER_PASSWORD)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.response.loginId").value(USER_ID))
			.andExpect(jsonPath("$.response.systemRole").value("USER"))
			.andExpect(jsonPath("$.response.mustChangePassword").value(false))
			.andReturn();

		final List<String> setCookies = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
		assertThat(setCookies).hasSize(2).allSatisfy(cookie -> assertThat(cookie).contains("HttpOnly", "SameSite=Lax"));
		assertThat(result.getResponse().getContentAsString()).doesNotContain(tokensFrom(result).access());

		final Tokens tokens = tokensFrom(result);
		mockMvc.perform(get("/api/auth/me").cookie(tokens.accessCookie()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.loginId").value(USER_ID));

		final List<LoginHistory> history = loginHistoryRepository.findAllByLoginIdOrderByIdAsc(USER_ID);
		assertThat(history).singleElement().satisfies(entry -> {
			assertThat(entry.getResult()).isEqualTo(LoginResult.SUCCESS);
			assertThat(entry.getIpAddress()).isEqualTo("127.0.0.1");
			assertThat(entry.getOccurredAt()).isEqualTo(TestClockConfig.START);
		});
	}

	@Test
	@DisplayName("비밀번호가 틀리면 400 INVALID_CREDENTIALS, 실패 사유와 IP를 기록한다")
	void wrongPasswordIsRejected() throws Exception {
		expectError(login(USER_ID, "Wrong-pass1"), ErrorCode.INVALID_CREDENTIALS);

		assertThat(loginHistoryRepository.findAllByLoginIdOrderByIdAsc(USER_ID)).singleElement().satisfies(entry -> {
			assertThat(entry.getResult()).isEqualTo(LoginResult.FAILURE);
			assertThat(entry.getFailureReason()).isEqualTo(LoginFailureReason.BAD_PASSWORD);
			assertThat(entry.getIpAddress()).isEqualTo("127.0.0.1");
		});
	}

	@Test
	@DisplayName("없는 아이디도 같은 오류로 응답하고(계정 존재 비노출), 로그에는 사유를 구분해 남긴다")
	void unknownLoginIdLooksLikeWrongPassword() throws Exception {
		expectError(login("nobody", USER_PASSWORD), ErrorCode.INVALID_CREDENTIALS);

		assertThat(loginHistoryRepository.findAllByLoginIdOrderByIdAsc("nobody")).singleElement().satisfies(entry -> {
			assertThat(entry.getUserId()).isNull();
			assertThat(entry.getFailureReason()).isEqualTo(LoginFailureReason.UNKNOWN_LOGIN_ID);
		});
	}

	@Test
	@DisplayName("입력값 검증: 아이디가 비면 400과 안내 메시지")
	void blankLoginIdIsRejected() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"loginId\":\"\",\"password\":\"x\"}"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.error.message").value("아이디를 입력해 주세요"));
	}

	@Test
	@DisplayName("로컬 로그인 정보가 없는 사용자는 로그인할 수 없고, 관리자 초기화로 발급하면 로그인된다")
	void userWithoutLocalCredential() throws Exception {
		final User noCredential = userRepository.save(User.create("sso01", "SSO 사용자", SystemRole.USER));

		expectError(login("sso01", USER_PASSWORD), ErrorCode.INVALID_CREDENTIALS);
		assertThat(loginHistoryRepository.findAllByLoginIdOrderByIdAsc("sso01")).singleElement()
			.satisfies(entry -> assertThat(entry.getFailureReason()).isEqualTo(LoginFailureReason.NO_LOCAL_CREDENTIAL));

		final Tokens admin = loginOk(ADMIN_ID, ADMIN_PASSWORD);
		final MvcResult reset = mockMvc.perform(
				post("/api/admin/users/{id}/password-reset", noCredential.getId()).cookie(admin.accessCookie()))
			.andExpect(status().isOk())
			.andReturn();
		final String temporaryPassword = readJson(reset).path("response").path("temporaryPassword").asString();

		login("sso01", temporaryPassword)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.mustChangePassword").value(true));
	}

	// ---- 5회 연속 실패 잠금 ----

	@Test
	@DisplayName("5회 연속 실패하면 잠기고(423), 이후에는 맞는 비밀번호로도 로그인할 수 없다")
	void locksAfterFiveConsecutiveFailures() throws Exception {
		for (int i = 0; i < 4; i++) {
			expectError(login(USER_ID, "Wrong-pass1"), ErrorCode.INVALID_CREDENTIALS);
		}
		expectError(login(USER_ID, "Wrong-pass1"), ErrorCode.ACCOUNT_LOCKED);
		expectError(login(USER_ID, USER_PASSWORD), ErrorCode.ACCOUNT_LOCKED);

		assertThat(findCredential(USER_ID).isLocked()).isTrue();
	}

	@Test
	@DisplayName("중간에 성공하면 연속 실패 횟수가 초기화된다")
	void successResetsConsecutiveFailures() throws Exception {
		for (int i = 0; i < 4; i++) {
			expectError(login(USER_ID, "Wrong-pass1"), ErrorCode.INVALID_CREDENTIALS);
		}
		loginOk(USER_ID, USER_PASSWORD);
		for (int i = 0; i < 4; i++) {
			expectError(login(USER_ID, "Wrong-pass1"), ErrorCode.INVALID_CREDENTIALS);
		}

		assertThat(findCredential(USER_ID).isLocked()).isFalse();
		assertThat(findCredential(USER_ID).getFailedLoginCount()).isEqualTo(4);
	}

	@Test
	@DisplayName("비활성 계정은 비밀번호가 맞아도 로그인할 수 없다(403)")
	void inactiveAccountCannotLogin() throws Exception {
		jdbcTemplate.update("update users set status = 'INACTIVE' where login_id = ?", USER_ID);

		expectError(login(USER_ID, USER_PASSWORD), ErrorCode.ACCOUNT_DISABLED);
		expectError(login(USER_ID, "Wrong-pass1"), ErrorCode.INVALID_CREDENTIALS);
	}

	// ---- 세션 만료 (30분 무활동) ----

	@Test
	@DisplayName("30분 동안 활동이 없으면 세션이 만료되어 재발급도 거부된다")
	void sessionExpiresAfterThirtyMinutesIdle() throws Exception {
		final Tokens tokens = loginOk(USER_ID, USER_PASSWORD);

		clock.advance(Duration.ofMinutes(31));

		expectError(mockMvc.perform(get("/api/auth/me").cookie(tokens.accessCookie())), ErrorCode.EXPIRED_TOKEN);
		expectError(refresh(tokens), ErrorCode.SESSION_EXPIRED);
	}

	@Test
	@DisplayName("활동이 있으면 로그인 후 30분이 지나도 세션이 유지된다")
	void activityKeepsSessionAlive() throws Exception {
		final Tokens tokens = loginOk(USER_ID, USER_PASSWORD);

		clock.advance(Duration.ofMinutes(9));
		mockMvc.perform(get("/api/auth/me").cookie(tokens.accessCookie())).andExpect(status().isOk());

		clock.advance(Duration.ofMinutes(9));    // 18분: AccessToken(10분) 만료 → 재발급
		Cookie access = refreshOk(tokens);
		mockMvc.perform(get("/api/auth/me").cookie(access)).andExpect(status().isOk());

		clock.advance(Duration.ofMinutes(18));   // 36분: 마지막 활동(18분) 후 18분
		access = refreshOk(tokens);
		mockMvc.perform(get("/api/auth/me").cookie(access)).andExpect(status().isOk());
	}

	@Test
	@DisplayName("토큰 재발급만으로는 무활동 만료가 연장되지 않는다")
	void refreshDoesNotExtendIdleTimeout() throws Exception {
		final Tokens tokens = loginOk(USER_ID, USER_PASSWORD);

		clock.advance(Duration.ofMinutes(20));
		refreshOk(tokens);

		clock.advance(Duration.ofMinutes(11));   // 로그인 후 31분, 그 사이 API 활동 없음
		expectError(refresh(tokens), ErrorCode.SESSION_EXPIRED);
	}

	// ---- 로그아웃 ----

	@Test
	@DisplayName("로그아웃하면 쿠키가 지워지고 같은 토큰으로 더는 접근할 수 없다")
	void logoutRevokesSession() throws Exception {
		final Tokens tokens = loginOk(USER_ID, USER_PASSWORD);

		final MvcResult result = mockMvc.perform(post("/api/auth/logout").cookie(tokens.accessCookie(), tokens.refreshCookie()))
			.andExpect(status().isOk())
			.andReturn();
		assertThat(result.getResponse().getHeaders(HttpHeaders.SET_COOKIE)).allSatisfy(cookie -> assertThat(cookie).contains("Max-Age=0"));

		expectError(mockMvc.perform(get("/api/auth/me").cookie(tokens.accessCookie())), ErrorCode.SESSION_EXPIRED);
		expectError(refresh(tokens), ErrorCode.SESSION_EXPIRED);
	}

	// ---- 비밀번호 변경 ----

	@Test
	@DisplayName("비밀번호 변경: 현재 비밀번호 불일치·규칙 위반·기존과 동일은 거부한다")
	void changePasswordValidation() throws Exception {
		final Tokens tokens = loginOk(USER_ID, USER_PASSWORD);

		expectError(changePassword(tokens, "Wrong-pass1", NEW_PASSWORD), ErrorCode.INVALID_PASSWORD);
		expectError(changePassword(tokens, USER_PASSWORD, "abcdefgh"), ErrorCode.INVALID_NEW_PASSWORD);
		expectError(changePassword(tokens, USER_PASSWORD, USER_PASSWORD), ErrorCode.SAME_AS_CURRENT_PASSWORD);
	}

	@Test
	@DisplayName("비밀번호를 바꾸면 기존 세션이 끊기고 새 비밀번호로만 로그인된다")
	void changePasswordRevokesSessions() throws Exception {
		final Tokens tokens = loginOk(USER_ID, USER_PASSWORD);
		final Tokens otherDevice = loginOk(USER_ID, USER_PASSWORD);

		changePassword(tokens, USER_PASSWORD, NEW_PASSWORD).andExpect(status().isOk());

		expectError(mockMvc.perform(get("/api/auth/me").cookie(tokens.accessCookie())), ErrorCode.SESSION_EXPIRED);
		expectError(mockMvc.perform(get("/api/auth/me").cookie(otherDevice.accessCookie())), ErrorCode.SESSION_EXPIRED);
		expectError(login(USER_ID, USER_PASSWORD), ErrorCode.INVALID_CREDENTIALS);
		loginOk(USER_ID, NEW_PASSWORD);

		assertThat(passwordEncoder.matches(NEW_PASSWORD, findCredential(USER_ID).getPasswordHash())).isTrue();
	}

	// ---- 관리자 비밀번호 초기화 ----

	@Test
	@DisplayName("일반 사용자는 관리자 비밀번호 초기화 API를 쓸 수 없다(404로 존재 비노출)")
	void nonAdminCannotResetPassword() throws Exception {
		final Tokens tokens = loginOk(USER_ID, USER_PASSWORD);
		final Long adminId = findUser(ADMIN_ID).getId();

		expectError(
			mockMvc.perform(post("/api/admin/users/{id}/password-reset", adminId).cookie(tokens.accessCookie())),
			ErrorCode.NOT_FOUND
		);
	}

	@Test
	@DisplayName("관리자 초기화: 잠금 해제 → 임시 비밀번호 로그인 → 변경 전까지 다른 기능 차단 → 변경 후 정상 이용")
	void adminResetFlow() throws Exception {
		for (int i = 0; i < 5; i++) {
			login(USER_ID, "Wrong-pass1");
		}
		assertThat(findCredential(USER_ID).isLocked()).isTrue();

		final Tokens admin = loginOk(ADMIN_ID, ADMIN_PASSWORD);
		final MvcResult resetResult = mockMvc.perform(
				post("/api/admin/users/{id}/password-reset", findUser(USER_ID).getId()).cookie(admin.accessCookie()))
			.andExpect(status().isOk())
			.andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
			.andReturn();
		final String temporaryPassword = readJson(resetResult).path("response").path("temporaryPassword").asString();
		assertThat(PasswordPolicy.isSatisfiedBy(temporaryPassword)).isTrue();
		assertThat(findCredential(USER_ID).isLocked()).isFalse();

		final MvcResult loginResult = login(USER_ID, temporaryPassword)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.mustChangePassword").value(true))
			.andReturn();
		final Tokens temp = tokensFrom(loginResult);

		mockMvc.perform(get("/api/auth/me").cookie(temp.accessCookie())).andExpect(status().isOk());
		expectError(
			mockMvc.perform(post("/api/admin/users/{id}/password-reset", 1L).cookie(temp.accessCookie())),
			ErrorCode.PASSWORD_CHANGE_REQUIRED
		);
		expectError(mockMvc.perform(get("/api/projects").cookie(temp.accessCookie())), ErrorCode.PASSWORD_CHANGE_REQUIRED);

		changePassword(temp, temporaryPassword, NEW_PASSWORD).andExpect(status().isOk());
		login(USER_ID, NEW_PASSWORD)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.mustChangePassword").value(false));
	}

	@Test
	@DisplayName("없는 사용자를 초기화하면 404 USER_NOT_FOUND")
	void resetUnknownUser() throws Exception {
		final Tokens admin = loginOk(ADMIN_ID, ADMIN_PASSWORD);

		expectError(
			mockMvc.perform(post("/api/admin/users/{id}/password-reset", 999_999L).cookie(admin.accessCookie())),
			ErrorCode.USER_NOT_FOUND
		);
	}

	// ---- helpers ----

	/** 본인이 비밀번호를 이미 정한(변경 강제 아님) 사용자와 로컬 로그인 정보를 만든다. */
	private void saveUser(final String loginId, final String password, final SystemRole role) {
		final User user = userRepository.save(User.create(loginId, loginId + " 이름", role));
		final LocalCredential credential = LocalCredential.issue(user.getId(), passwordEncoder.encode(password), clock.instant());
		credential.changePassword(passwordEncoder.encode(password), clock.instant());
		credentialRepository.save(credential);
	}

	private User findUser(final String loginId) {
		return userRepository.findByLoginId(loginId).orElseThrow();
	}

	private LocalCredential findCredential(final String loginId) {
		return credentialRepository.findById(findUser(loginId).getId()).orElseThrow();
	}

	private ResultActions login(final String loginId, final String password) throws Exception {
		final String body = jsonMapper.writeValueAsString(Map.of("loginId", loginId, "password", password));
		return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private Tokens loginOk(final String loginId, final String password) throws Exception {
		return tokensFrom(login(loginId, password).andExpect(status().isOk()).andReturn());
	}

	private ResultActions refresh(final Tokens tokens) throws Exception {
		return mockMvc.perform(post("/api/auth/refresh").cookie(tokens.refreshCookie()));
	}

	private Cookie refreshOk(final Tokens tokens) throws Exception {
		final MvcResult result = refresh(tokens).andExpect(status().isOk()).andReturn();
		return new Cookie(AuthCookieManager.ACCESS_TOKEN_COOKIE, cookieValue(result, AuthCookieManager.ACCESS_TOKEN_COOKIE));
	}

	private ResultActions changePassword(final Tokens tokens, final String current, final String next) throws Exception {
		final String body = jsonMapper.writeValueAsString(Map.of("currentPassword", current, "newPassword", next));
		return mockMvc.perform(patch("/api/users/me/password")
			.cookie(tokens.accessCookie())
			.contentType(MediaType.APPLICATION_JSON)
			.content(body));
	}

	private void expectError(final ResultActions actions, final ErrorCode errorCode) throws Exception {
		actions
			.andExpect(status().is(errorCode.getStatus()))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.status").value(errorCode.getStatus()))
			.andExpect(jsonPath("$.error.message").value(errorCode.getMessage()));
	}

	private JsonNode readJson(final MvcResult result) throws Exception {
		return jsonMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
	}

	private static Tokens tokensFrom(final MvcResult result) {
		return new Tokens(
			cookieValue(result, AuthCookieManager.ACCESS_TOKEN_COOKIE),
			cookieValue(result, AuthCookieManager.REFRESH_TOKEN_COOKIE)
		);
	}

	/** Set-Cookie 헤더에서 값만 읽는다(속성 검증은 각 테스트에서 헤더 문자열로 한다). */
	private static String cookieValue(final MvcResult result, final String name) {
		return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
			.filter(header -> header.startsWith(name + "="))
			.map(header -> header.substring(name.length() + 1, header.indexOf(';')))
			.findFirst()
			.orElseThrow(() -> new AssertionError(name + " 쿠키가 발급되지 않았습니다"));
	}
}
