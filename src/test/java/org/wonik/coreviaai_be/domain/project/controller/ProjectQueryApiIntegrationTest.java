package org.wonik.coreviaai_be.domain.project.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility;
import org.wonik.coreviaai_be.domain.project.repository.ProjectRepository;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * FR-PRJ-01 목록·상세의 공개 범위(요구사항 명세서 3.1·3.2: 권한 밖 프로젝트 미표시)와 필터·커서 페이지를 검증한다.
 * <p>
 * 준비: 개발팀 주관으로 전사 공개(COMPANY_ID), 부서 공개(DEPT_ID), 멤버 전용(MEMBERS_ID, 영업팀 현업 담당자 초대) 세 개.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectQueryApiIntegrationTest extends ProjectApiTestSupport {

	@Autowired
	private ProjectRepository projectRepository;

	private long companyId;
	private long departmentOnlyId;
	private long membersOnlyId;

	@BeforeEach
	void createProjects() throws Exception {
		companyId = createProject(project("COM-1", "COMPANY", List.of()));
		departmentOnlyId = createProject(project("DEP-1", "DEPARTMENT", List.of()));
		membersOnlyId = createProject(project("MEM-1", "MEMBERS_ONLY", List.of(member(businessOwner.getId(), "BUSINESS_OWNER"))));
	}

	@Test
	@DisplayName("일반 사용자: 전사 공개 + 내 부서의 부서 공개 + 내가 멤버인 프로젝트만 보인다")
	void userVisibility() throws Exception {
		assertThat(visibleIds(loginAs(developer))).containsExactly(departmentOnlyId, companyId);
		assertThat(visibleIds(loginAs(businessOwner))).containsExactly(membersOnlyId, companyId);
		assertThat(visibleIds(loginAs(creator))).containsExactly(membersOnlyId, departmentOnlyId, companyId);
	}

	@Test
	@DisplayName("경영진: 소속 부서와 하위 부서 프로젝트는 멤버 전용까지 보이고, 그 밖은 전사 공개만 보인다")
	void executiveVisibility() throws Exception {
		assertThat(visibleIds(loginAs(itExecutive))).containsExactly(membersOnlyId, departmentOnlyId, companyId);
		assertThat(visibleIds(loginAs(salesExecutive))).containsExactly(companyId);

		final User noDepartmentExecutive = saveUser("exec00", USER_PASSWORD, SystemRole.EXECUTIVE, null);
		assertThat(visibleIds(loginAs(noDepartmentExecutive))).containsExactly(companyId);
	}

	@Test
	@DisplayName("상세: 볼 수 없는 프로젝트는 존재를 드러내지 않고 404, 볼 수 있으면 운영 방식을 준다")
	void detailVisibility() throws Exception {
		expectError(getAs(loginAs(developer), PROJECTS + "/{id}", membersOnlyId), ErrorCode.PROJECT_NOT_FOUND);
		expectError(getAs(loginAs(salesExecutive), PROJECTS + "/{id}", departmentOnlyId), ErrorCode.PROJECT_NOT_FOUND);
		expectError(getAs(loginAs(developer), PROJECTS + "/{id}", 999_999L), ErrorCode.PROJECT_NOT_FOUND);

		getAs(loginAs(itExecutive), PROJECTS + "/{id}", membersOnlyId)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.methodology").value("WATERFALL"))
			.andExpect(jsonPath("$.response.visibility").value("MEMBERS_ONLY"))
			.andExpect(jsonPath("$.response.phases").doesNotExist());
	}

	@Test
	@DisplayName("단계 API: 상세와 같은 공개 범위를 따르고, 단계 순서대로 5개를 준다")
	void phasesVisibility() throws Exception {
		assertThat(phasesOf(loginAs(itExecutive), membersOnlyId).valueStream().map(p -> p.get("phaseOrder").asInt()).toList())
			.containsExactly(1, 2, 3, 4, 5);
		expectError(getAs(loginAs(developer), PROJECTS + "/{id}/phases", membersOnlyId), ErrorCode.PROJECT_NOT_FOUND);
		expectError(getAs(loginAs(developer), PROJECTS + "/{id}/phases", 999_999L), ErrorCode.PROJECT_NOT_FOUND);
	}

	@Test
	@DisplayName("단계 API: 단계가 없는 운영 방식(애자일)의 프로젝트는 PHASES_NOT_AVAILABLE")
	void phasesNotAvailableForNonWaterfall() throws Exception {
		// MVP에서는 API로 애자일 프로젝트를 만들 수 없어 저장소로 직접 만든다(FR-PRJ-10 이후 대비).
		final Project agile = projectRepository.save(Project.create(
			"AGL-1", "애자일 프로젝트", LocalDate.of(2026, 12, 1), LocalDate.of(2027, 3, 31), devTeam.getId(),
			ProjectVisibility.COMPANY, Methodology.AGILE, DepartmentType.SW, creator.getId()));

		expectError(getAs(loginAs(developer), PROJECTS + "/{id}/phases", agile.getId()), ErrorCode.PHASES_NOT_AVAILABLE);
	}

	@Test
	@DisplayName("목록 항목에 주관 부서명·PM 이름이 나오고, 상태·주관 부서·PM으로 거를 수 있다")
	void listFieldsAndFilters() throws Exception {
		final Cookie cookie = loginAs(creator);
		getAs(cookie, PROJECTS + "?size=1")
			.andExpect(jsonPath("$.response.items[0].code").value("MEM-1"))
			.andExpect(jsonPath("$.response.items[0].departmentName").value("개발팀"))
			.andExpect(jsonPath("$.response.items[0].pmUserId").value(creator.getId()))
			.andExpect(jsonPath("$.response.items[0].pmName").value(creator.getName()))
			.andExpect(jsonPath("$.response.items[0].methodology").value("WATERFALL"));

		assertThat(visibleIds(cookie, "&status=PLANNED")).hasSize(3);
		assertThat(visibleIds(cookie, "&status=COMPLETED")).isEmpty();
		assertThat(visibleIds(cookie, "&departmentId=" + devTeam.getId())).hasSize(3);
		assertThat(visibleIds(cookie, "&departmentId=" + salesTeam.getId())).isEmpty();
		assertThat(visibleIds(cookie, "&pmUserId=" + developer.getId())).isEmpty();
	}

	@Test
	@DisplayName("커서 페이지: 최신 생성순으로 끊어 읽고, 마지막 페이지는 hasNext=false")
	void cursorPaging() throws Exception {
		final Cookie cookie = loginAs(creator);
		final JsonNode first = readResponse(getAs(cookie, PROJECTS + "?size=2"));
		assertThat(ids(first)).containsExactly(membersOnlyId, departmentOnlyId);
		assertThat(first.get("hasNext").asBoolean()).isTrue();

		final JsonNode second = readResponse(getAs(cookie, PROJECTS + "?size=2&cursor=" + first.get("nextCursor").asString()));
		assertThat(ids(second)).containsExactly(companyId);
		assertThat(second.get("hasNext").asBoolean()).isFalse();

		expectError(getAs(cookie, PROJECTS + "?cursor=broken"), ErrorCode.INVALID_CURSOR);
		getAs(cookie, PROJECTS + "?size=101").andExpect(status().isBadRequest());
	}

	private Map<String, Object> project(final String code, final String visibility, final List<Map<String, Object>> members) {
		final Map<String, Object> body = validCreateBody(code);
		body.put("visibility", visibility);
		body.put("members", members);
		body.put("gateApproverUserId", null);
		return body;
	}

	private List<Long> visibleIds(final Cookie cookie) throws Exception {
		return visibleIds(cookie, "");
	}

	private List<Long> visibleIds(final Cookie cookie, final String query) throws Exception {
		return ids(readResponse(getAs(cookie, PROJECTS + "?size=100" + query)));
	}

	private static List<Long> ids(final JsonNode page) {
		return page.get("items").valueStream().map(item -> item.get("id").asLong()).toList();
	}
}
