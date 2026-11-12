package org.wonik.coreviaai_be.domain.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.wonik.authserver.domain.credential.entity.LocalCredential;
import org.wonik.authserver.domain.credential.repository.LocalCredentialRepository;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditLog;
import org.wonik.global.audit.AuditLogRepository;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.department.entity.Department;
import org.wonik.global.department.repository.DepartmentRepository;
import org.wonik.global.jwt.AuthCookieManager;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;
import org.wonik.support.InMemoryAuthSessionStore;
import org.wonik.support.MutableClock;
import org.wonik.support.TestClockConfig;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 관리자 API 통합 테스트 공통: 데이터 초기화, 사용자·부서 준비, 로그인, JSON 요청, 오류 검증.
 * 매 테스트 전에 시스템 관리자(ADMIN_ID) 한 명을 만들고 로그인해 둔다({@link #adminCookie}).
 */
abstract class AdminApiTestSupport {

	protected static final String ADMIN_ID = "admin01";
	protected static final String ADMIN_PASSWORD = "Adm1n-pass!";
	protected static final String USER_PASSWORD = "Passw0rd!";

	@Autowired
	protected MockMvc mockMvc;
	@Autowired
	protected JsonMapper jsonMapper;
	@Autowired
	protected UserRepository userRepository;
	@Autowired
	protected DepartmentRepository departmentRepository;
	@Autowired
	protected LocalCredentialRepository credentialRepository;
	@Autowired
	protected AuditLogRepository auditLogRepository;
	@Autowired
	protected PasswordEncoder passwordEncoder;
	@Autowired
	protected JdbcTemplate jdbcTemplate;
	@Autowired
	protected InMemoryAuthSessionStore sessionStore;
	@Autowired
	protected MutableClock clock;

	protected Long adminId;
	protected Cookie adminCookie;

	@BeforeEach
	void resetData() throws Exception {
		clock.setInstant(TestClockConfig.START);
		sessionStore.clear();
		// FK 순서대로 비운다. 부서는 상위 참조를 먼저 끊고 지운다.
		jdbcTemplate.update("DELETE FROM audit_logs");
		jdbcTemplate.update("DELETE FROM login_history");
		jdbcTemplate.update("DELETE FROM local_credentials");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("UPDATE departments SET parent_id = NULL");
		jdbcTemplate.update("DELETE FROM departments");

		adminId = saveUser(ADMIN_ID, ADMIN_PASSWORD, SystemRole.SYSTEM_ADMIN, null).getId();
		adminCookie = loginOk(ADMIN_ID, ADMIN_PASSWORD);
	}

	// ---- 데이터 준비 ----

	/** 비밀번호를 이미 정한(변경 강제 아님) 사용자와 로컬 로그인 정보를 만든다. */
	protected User saveUser(final String loginId, final String password, final SystemRole role, final Long departmentId) {
		final User user = userRepository.save(User.create(loginId, loginId + " 이름", role, departmentId, null));
		final LocalCredential credential = LocalCredential.issue(user.getId(), passwordEncoder.encode(password), clock.instant());
		credential.changePassword(passwordEncoder.encode(password), clock.instant());
		credentialRepository.save(credential);
		return user;
	}

	protected Department saveDepartment(final String name, final Long parentId) {
		return departmentRepository.save(Department.create(parentId, name));
	}

	protected User findUser(final Long userId) {
		return userRepository.findById(userId).orElseThrow();
	}

	protected List<AuditLog> auditLogs(final AuditAction action) {
		return auditLogRepository.findAll().stream()
			.filter(log -> log.getAction() == action)
			.toList();
	}

	// ---- 요청 ----

	protected ResultActions login(final String loginId, final String password) throws Exception {
		return postJson("/api/auth/login", null, body("loginId", loginId, "password", password));
	}

	protected Cookie loginOk(final String loginId, final String password) throws Exception {
		final MvcResult result = login(loginId, password).andExpect(status().isOk()).andReturn();
		final String value = result.getResponse().getHeaders(HttpHeaders.SET_COOKIE).stream()
			.filter(header -> header.startsWith(AuthCookieManager.ACCESS_TOKEN_COOKIE + "="))
			.map(header -> header.substring(AuthCookieManager.ACCESS_TOKEN_COOKIE.length() + 1, header.indexOf(';')))
			.findFirst()
			.orElseThrow(() -> new AssertionError("AccessToken 쿠키가 발급되지 않았습니다"));
		return new Cookie(AuthCookieManager.ACCESS_TOKEN_COOKIE, value);
	}

	protected ResultActions getAs(final Cookie cookie, final String url, final Object... uriVariables) throws Exception {
		return mockMvc.perform(withCookie(get(url, uriVariables), cookie));
	}

	protected ResultActions postJson(final String url, final Cookie cookie, final Map<String, Object> body) throws Exception {
		return mockMvc.perform(withCookie(post(url), cookie)
			.contentType(MediaType.APPLICATION_JSON)
			.content(jsonMapper.writeValueAsString(body)));
	}

	protected ResultActions patchJson(
		final Cookie cookie,
		final Map<String, Object> body,
		final String url,
		final Object... uriVariables
	) throws Exception {
		return mockMvc.perform(withCookie(patch(url, uriVariables), cookie)
			.contentType(MediaType.APPLICATION_JSON)
			.content(jsonMapper.writeValueAsString(body)));
	}

	/** null 값을 넣을 수 있는 요청 본문(Map.of는 null을 허용하지 않는다). 키·값을 번갈아 넘긴다. */
	protected static Map<String, Object> body(final Object... keyValues) {
		final Map<String, Object> body = new LinkedHashMap<>();
		for (int i = 0; i < keyValues.length; i += 2) {
			body.put((String)keyValues[i], keyValues[i + 1]);
		}
		return body;
	}

	protected void expectError(final ResultActions actions, final ErrorCode errorCode) throws Exception {
		actions
			.andExpect(status().is(errorCode.getStatus()))
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.error.status").value(errorCode.getStatus()))
			.andExpect(jsonPath("$.error.message").value(errorCode.getMessage()));
	}

	protected JsonNode readResponse(final ResultActions actions) throws Exception {
		final MvcResult result = actions.andExpect(status().isOk()).andReturn();
		return jsonMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("response");
	}

	private static MockHttpServletRequestBuilder withCookie(final MockHttpServletRequestBuilder builder, final Cookie cookie) {
		return cookie == null ? builder : builder.cookie(cookie);
	}
}
