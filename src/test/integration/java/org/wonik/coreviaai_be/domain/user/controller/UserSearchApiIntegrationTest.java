package org.wonik.coreviaai_be.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.support.ApiIntegrationTestSupport;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * 일반 사용자용 사용자 이름 검색(GET /api/users/search)을 실제 보안 필터와 함께 HTTP 경계에서 검증한다.
 * 보안 브리프 검증 계획: 인증·시스템 관리자 차단, 검색어·size 검증, 와일드카드 이스케이프, 초대 가능 대상만, 응답 필드 최소화, 상한, 정렬 안정성.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class UserSearchApiIntegrationTest extends ApiIntegrationTestSupport {

	private static final String SEARCH = UserSearchController.PATH;

	private Department devTeam;
	private User requester;
	private Cookie userCookie;

	@BeforeEach
	void login() throws Exception {
		devTeam = saveDepartment("개발팀", null);
		requester = saveUser("user01", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		rename(requester, "검색자 김민수", devTeam.getId(), null);
		userCookie = loginOk("user01", USER_PASSWORD);
	}

	@Test
	@DisplayName("이름 부분 일치(대소문자 무시)로 활성 일반 사용자의 id·이름·부서명·직무만 준다")
	void searchesByNameWithMinimalFields() throws Exception {
		final User kim = saveTarget("kim01", "김철수", SystemRole.USER, devTeam.getId(), "백엔드 개발");
		saveTarget("lee01", "이영희", SystemRole.USER, devTeam.getId(), null);
		final User alice = saveTarget("alice", "Alice Kim", SystemRole.USER, null, null);

		search("김철")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.response.length()").value(1))
			.andExpect(jsonPath("$.response[0].id").value(kim.getId()))
			.andExpect(jsonPath("$.response[0].name").value("김철수"))
			.andExpect(jsonPath("$.response[0].departmentName").value("개발팀"))
			.andExpect(jsonPath("$.response[0].jobTitle").value("백엔드 개발"))
			.andExpect(jsonPath("$.response[0].length()").value(4))
			.andExpect(jsonPath("$.response[0].loginId").doesNotExist())
			.andExpect(jsonPath("$.response[0].systemRole").doesNotExist())
			.andExpect(jsonPath("$.response[0].status").doesNotExist());

		// 대소문자 무시, 부서·직무가 없으면 null
		search("KIM")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.length()").value(1))
			.andExpect(jsonPath("$.response[0].id").value(alice.getId()))
			.andExpect(jsonPath("$.response[0].departmentName").doesNotExist())
			.andExpect(jsonPath("$.response[0].jobTitle").doesNotExist());

		// 일치하는 사용자가 없으면 빈 목록
		search("없는이름")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.length()").value(0));
	}

	@Test
	@DisplayName("비활성·경영진·시스템 관리자·본인은 결과에서 빠진다")
	void excludesNonInvitableAndSelf() throws Exception {
		final User active = saveTarget("u-active", "박민수 활성", SystemRole.USER, devTeam.getId(), null);
		final User inactive = saveTarget("u-inactive", "박민수 비활성", SystemRole.USER, devTeam.getId(), null);
		inactive.deactivate();
		userRepository.save(inactive);
		saveTarget("u-exec", "박민수 경영진", SystemRole.EXECUTIVE, devTeam.getId(), null);
		saveTarget("u-admin", "박민수 관리자", SystemRole.SYSTEM_ADMIN, null, null);

		search("박민수")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.length()").value(1))
			.andExpect(jsonPath("$.response[0].id").value(active.getId()));

		// 본인 이름(검색자 김민수)과 일치해도 본인은 빠진다
		search("김민수")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.length()").value(0));
	}

	@Test
	@DisplayName("%, _, 역슬래시는 와일드카드가 아니라 문자 그대로 매칭된다")
	void wildcardsAreLiteral() throws Exception {
		final User percent = saveTarget("p1", "목표100%달성", SystemRole.USER, null, null);
		saveTarget("p2", "목표1000달성", SystemRole.USER, null, null);
		final User underscore = saveTarget("p3", "김_철수", SystemRole.USER, null, null);
		saveTarget("p4", "김a철수", SystemRole.USER, null, null);
		final User backslash = saveTarget("p5", "a\\b 사용자", SystemRole.USER, null, null);
		saveTarget("p6", "ab 사용자", SystemRole.USER, null, null);

		expectIds(search("0%"), percent.getId());
		expectIds(search("김_"), underscore.getId());
		expectIds(search("a\\b"), backslash.getId());
		// 와일드카드만으로 전체를 조회할 수 없다
		expectIds(search("%%"));
		expectIds(search("__"));
	}

	@Test
	@DisplayName("결과는 기본·최대 20건이고 이름 오름차순, 같은 이름은 ID 오름차순이다")
	void limitsAndSortsStably() throws Exception {
		final List<Long> sameNameIds = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			sameNameIds.add(saveTarget("same" + i, "가동명이인", SystemRole.USER, null, null).getId());
		}
		for (int i = 0; i < 20; i++) {
			saveTarget("many" + i, String.format("나동명%02d", i), SystemRole.USER, null, null);
		}

		final JsonNode all = readResponse(search("동명"));
		assertThat(all.size()).isEqualTo(20);
		// 이름순: 가동명이인(3명, ID순) → 나동명00 ...
		for (int i = 0; i < 3; i++) {
			assertThat(all.get(i).get("id").asLong()).isEqualTo(sameNameIds.get(i));
		}
		assertThat(all.get(3).get("name").asString()).isEqualTo("나동명00");
		assertThat(all.get(19).get("name").asString()).isEqualTo("나동명16");

		// size 경계: 1건, 20건
		final JsonNode one = readResponse(search("동명", "1"));
		assertThat(one.size()).isEqualTo(1);
		assertThat(one.get(0).get("id").asLong()).isEqualTo(sameNameIds.get(0));
		assertThat(readResponse(search("동명", "20")).size()).isEqualTo(20);

		// 같은 요청을 반복해도 순서가 같다
		assertThat(readResponse(search("동명"))).isEqualTo(all);
	}

	@Test
	@DisplayName("검색어는 앞뒤 공백을 빼고 2~50자여야 하며 1자·51자·공백만·제어 문자·누락은 400")
	void validatesKeyword() throws Exception {
		final User target = saveTarget("t1", "홍길동", SystemRole.USER, null, null);
		expectIds(search("  홍길  "), target.getId());
		search("가".repeat(50)).andExpect(status().isOk());

		expectError(search("홍"), ErrorCode.INVALID_USER_SEARCH_KEYWORD);
		expectError(search(" 홍 "), ErrorCode.INVALID_USER_SEARCH_KEYWORD);
		expectError(search("가".repeat(51)), ErrorCode.INVALID_USER_SEARCH_KEYWORD);
		expectError(search("    "), ErrorCode.INVALID_USER_SEARCH_KEYWORD);
		expectError(search(""), ErrorCode.INVALID_USER_SEARCH_KEYWORD);
		expectError(search("홍\u0000길"), ErrorCode.INVALID_USER_SEARCH_KEYWORD);
		expectError(search("홍\n길"), ErrorCode.INVALID_USER_SEARCH_KEYWORD);
		mockMvc.perform(get(SEARCH).cookie(userCookie)).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("size는 1~20만 허용하고 0·21·음수·숫자 아님은 400")
	void validatesSize() throws Exception {
		search("홍길", "0").andExpect(status().isBadRequest());
		search("홍길", "21").andExpect(status().isBadRequest());
		search("홍길", "-1").andExpect(status().isBadRequest());
		search("홍길", "abc").andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("비로그인은 401, 시스템 관리자는 프로젝트 API와 같이 404로 막고, 경영진은 조회할 수 있다")
	void accessControl() throws Exception {
		saveTarget("t1", "홍길동", SystemRole.USER, null, null);
		expectError(mockMvc.perform(get(SEARCH).param("keyword", "홍길")), ErrorCode.UNAUTHORIZED);

		saveUser("admin01", USER_PASSWORD, SystemRole.SYSTEM_ADMIN, null);
		final Cookie adminCookie = loginOk("admin01", USER_PASSWORD);
		expectError(mockMvc.perform(get(SEARCH).cookie(adminCookie).param("keyword", "홍길")), ErrorCode.NOT_FOUND);

		saveUser("exec01", USER_PASSWORD, SystemRole.EXECUTIVE, devTeam.getId());
		final Cookie execCookie = loginOk("exec01", USER_PASSWORD);
		mockMvc.perform(get(SEARCH).cookie(execCookie).param("keyword", "홍길"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.length()").value(1));
	}

	@Test
	@DisplayName("검색어는 성공·실패 모두 로그에 남지 않는다")
	void keywordIsNotLogged(final CapturedOutput output) throws Exception {
		saveTarget("t1", "로그확인대상", SystemRole.USER, null, null);
		search("로그확인대상").andExpect(status().isOk());
		search("로그확인대상\u0000").andExpect(status().isBadRequest());
		search("로그확인대상", "99").andExpect(status().isBadRequest());

		assertThat(output.getAll()).doesNotContain("로그확인대상");
	}

	// ---- 도움 ----

	private ResultActions search(final String keyword) throws Exception {
		return mockMvc.perform(get(SEARCH).cookie(userCookie).param("keyword", keyword));
	}

	private ResultActions search(final String keyword, final String size) throws Exception {
		return mockMvc.perform(get(SEARCH).cookie(userCookie).param("keyword", keyword).param("size", size));
	}

	private void expectIds(final ResultActions actions, final Long... ids) throws Exception {
		final JsonNode response = readResponse(actions);
		final List<Long> actual = new ArrayList<>();
		response.forEach(node -> actual.add(node.get("id").asLong()));
		assertThat(actual).containsExactly(ids);
	}

	/** 검색 대상(로그인하지 않는 사용자). 로그인 정보 없이 사용자만 만든다. */
	private User saveTarget(
		final String loginId,
		final String name,
		final SystemRole role,
		final Long departmentId,
		final String jobTitle
	) {
		return userRepository.save(User.create(loginId, name, role, departmentId, jobTitle));
	}

	private void rename(final User user, final String name, final Long departmentId, final String jobTitle) {
		final User saved = findUser(user.getId());
		saved.changeProfile(name, departmentId, jobTitle);
		userRepository.save(saved);
	}
}
