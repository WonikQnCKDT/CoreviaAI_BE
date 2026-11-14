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

	/** 생성 권한자로 프로젝트를 만들고 ID를 돌려준다. */
	protected long createProject(final Map<String, Object> body) throws Exception {
		return readResponse(create(loginAs(creator), body)).get("id").asLong();
	}
}
