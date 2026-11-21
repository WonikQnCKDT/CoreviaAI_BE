package org.wonik.coreviaai_be.domain.project.controller;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.ResultActions;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.support.ApiIntegrationTestSupport;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * 프로젝트 API 통합 테스트 공통. 부서 트리와 역할별 사용자를 준비한다.
 * <pre>
 * IT사업부(division) ─ 개발팀(devTeam)      : creator(생성 권한), developer, noPermission
 *                    경영진 itExecutive(division 소속)
 * 영업사업부(salesDivision) ─ 영업팀(salesTeam): businessOwner(현업 담당자 후보)
 *                    경영진 salesExecutive(salesDivision 소속)
 * </pre>
 */
abstract class ProjectApiTestSupport extends ApiIntegrationTestSupport {

	protected static final String PROJECTS = "/api/projects";

	protected Department division;
	protected Department devTeam;
	protected Department salesDivision;
	protected Department salesTeam;

	protected User creator;
	protected User developer;
	protected User noPermission;
	protected User businessOwner;
	protected User itExecutive;
	protected User salesExecutive;

	@BeforeEach
	void prepareOrganization() {
		division = saveDepartment("IT사업부", null);
		devTeam = saveDepartment("개발팀", division.getId());
		salesDivision = saveDepartment("영업사업부", null);
		salesTeam = saveDepartment("영업팀", salesDivision.getId());

		creator = saveUser("creator01", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		creator.grantProjectCreation();
		creator = userRepository.save(creator);
		developer = saveUser("dev01", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		noPermission = saveUser("noperm01", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		businessOwner = saveUser("owner01", USER_PASSWORD, SystemRole.USER, salesTeam.getId());
		itExecutive = saveUser("itexec01", USER_PASSWORD, SystemRole.EXECUTIVE, division.getId());
		salesExecutive = saveUser("salesexec01", USER_PASSWORD, SystemRole.EXECUTIVE, salesDivision.getId());
	}

	protected Cookie loginAs(final User user) throws Exception {
		return loginOk(user.getLoginId(), USER_PASSWORD);
	}

	/**
	 * 통과하는 생성 요청 본문: 폭포수·SW, 액터 PM·현업 담당자·개발자, 멤버로 개발자와 현업 담당자,
	 * 게이트 승인자는 현업 담당자. 테스트는 필요한 값만 바꿔 쓴다.
	 */
	protected Map<String, Object> validCreateBody(final String code) {
		return body(
			"name", "구매 발주 시스템 구축",
			"code", code,
			"startDate", "2026-12-01",
			"endDate", "2027-03-31",
			"departmentId", devTeam.getId(),
			"visibility", "COMPANY",
			"methodology", "WATERFALL",
			"departmentType", "SW",
			"actors", List.of("PM", "BUSINESS_OWNER", "DEVELOPER"),
			"members", List.of(
				member(developer.getId(), "DEVELOPER"),
				member(businessOwner.getId(), "BUSINESS_OWNER")
			),
			"gateApproverUserId", businessOwner.getId()
		);
	}

	protected static Map<String, Object> member(final Long userId, final String actor) {
		return body("userId", userId, "actor", actor);
	}

	protected ResultActions create(final Cookie cookie, final Map<String, Object> body) throws Exception {
		return postJson(PROJECTS, cookie, body);
	}

	/** 폭포수 단계·게이트 목록(GET /api/projects/{id}/phases)의 응답 배열. */
	protected JsonNode phasesOf(final Cookie cookie, final long projectId) throws Exception {
		return readResponse(getAs(cookie, PROJECTS + "/{id}/phases", projectId));
	}

	/** 멤버 목록 첫 페이지(GET /api/projects/{id}/members, 최대 크기)의 항목 배열. */
	protected JsonNode membersOf(final Cookie cookie, final long projectId) throws Exception {
		return readResponse(getAs(cookie, PROJECTS + "/{id}/members?size=100", projectId)).get("items");
	}

	/** 멤버 목록을 "userId:역할" 문자열로(등록순). 순서·역할을 한 번에 비교하기 위한 형태다. */
	protected List<String> memberRoles(final Cookie cookie, final long projectId) throws Exception {
		return membersOf(cookie, projectId).valueStream()
			.map(member -> member.get("userId").asLong() + ":" + member.get("actor").asString())
			.toList();
	}

	/** 멤버 이력 첫 페이지(최신순, 최대 크기)의 항목 배열. */
	protected JsonNode historiesOf(final Cookie cookie, final long projectId) throws Exception {
		return readResponse(getAs(cookie, PROJECTS + "/{id}/members/histories?size=100", projectId)).get("items");
	}

	/** JSON 문자열 값. null이면 "null"로 바꿔 전·후 비교 문자열에 넣는다. */
	protected static String text(final JsonNode node) {
		return node == null || node.isNull() ? "null" : node.asString();
	}

	/** 사용자를 비활성화한다(퇴사 등). 관리자 API 대신 저장소로 바꿔 프로젝트 테스트의 준비를 짧게 한다. */
	protected void deactivate(final User user) {
		final User saved = findUser(user.getId());
		saved.deactivate();
		userRepository.save(saved);
	}

	/** 생성 권한자로 프로젝트를 만들고 ID를 돌려준다. */
	protected long createProject(final Map<String, Object> body) throws Exception {
		return readResponse(create(loginAs(creator), body)).get("id").asLong();
	}
}
