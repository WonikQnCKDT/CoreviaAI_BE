package org.wonik.coreviaai_be.domain.project.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.coreviaai_be.domain.project.repository.ProjectRepository;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

/**
 * FR-PRJ-01 프로젝트 생성을 HTTP 경계에서 검증한다(인수 기준 단위로 테스트를 나눈다).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectCreateApiIntegrationTest extends ProjectApiTestSupport {

	@Autowired
	private ProjectRepository projectRepository;

	@Test
	@DisplayName("폭포수로 생성하면 생성자가 PM이 되고, 5단계·게이트가 만들어지며 승인자가 모든 게이트에 적용된다")
	void createWaterfallProject() throws Exception {
		final Cookie cookie = loginAs(creator);
		final JsonNode project = readResponse(create(cookie, validCreateBody("PUR-2026")));

		assertThat(project.get("code").asString()).isEqualTo("PUR-2026");
		assertThat(project.get("methodology").asString()).isEqualTo("WATERFALL");
		assertThat(project.get("status").asString()).isEqualTo("PLANNED");
		assertThat(project.get("pmUserId").asLong()).isEqualTo(creator.getId());
		assertThat(project.get("createdBy").asLong()).isEqualTo(creator.getId());
		assertThat(project.get("departmentName").asString()).isEqualTo("개발팀");
		assertThat(project.get("actors").valueStream().map(JsonNode::asString).toList())
			.containsExactly("PM", "BUSINESS_OWNER", "DEVELOPER");
		// 멤버는 별도 커서 목록으로 조회한다. 등록순(id 오름차순)이라 생성자 PM이 먼저, 초대 멤버는 userId 순으로 저장된다.
		assertThat(project.has("members")).isFalse();
		assertThat(membersOf(cookie, project.get("id").asLong()).valueStream()
			.map(m -> m.get("userId").asLong() + ":" + m.get("actor").asString()).toList())
			.containsExactly(
				creator.getId() + ":PM",
				developer.getId() + ":DEVELOPER",
				businessOwner.getId() + ":BUSINESS_OWNER"
			);

		// 상세는 운영 방식과 무관한 공통 정보만 담고, 단계·게이트는 별도 API로 조회한다.
		assertThat(project.has("phases")).isFalse();
		final JsonNode phases = phasesOf(cookie, project.get("id").asLong());
		assertThat(phases.valueStream().map(p -> p.get("phaseType").asString()).toList())
			.containsExactly("ANALYSIS", "DESIGN", "DEVELOPMENT", "TEST", "TRANSITION");
		assertThat(phases.valueStream().toList()).allSatisfy(phase -> {
			assertThat(phase.get("status").asString()).isEqualTo("NOT_STARTED");
			assertThat(phase.get("gateStatus").asString()).isEqualTo("PENDING");
			assertThat(phase.get("gateApproverUserId").asLong()).isEqualTo(businessOwner.getId());
			assertThat(phase.get("gateApproverName").asString()).isEqualTo(businessOwner.getName());
		});

		assertThat(auditLogs(AuditAction.PROJECT_CREATED)).singleElement().satisfies(log -> {
			assertThat(log.getTargetType()).isEqualTo(AuditTargetType.PROJECT);
			assertThat(log.getTargetId()).isEqualTo(project.get("id").asLong());
			assertThat(log.getActorUserId()).isEqualTo(creator.getId());
		});
	}

	@Test
	@DisplayName("④ 멤버와 ⑤ 게이트 승인자는 선택이다: 생략하면 PM만 멤버이고 게이트 승인자는 미지정이다")
	void membersAndApproverAreOptional() throws Exception {
		final Map<String, Object> body = validCreateBody("ERP-1");
		body.remove("members");
		body.put("gateApproverUserId", null);
		body.put("actors", List.of("PM"));

		final Cookie cookie = loginAs(creator);
		final JsonNode project = readResponse(create(cookie, body));

		final JsonNode members = membersOf(cookie, project.get("id").asLong());
		assertThat(members).hasSize(1);
		assertThat(members.get(0).get("actor").asString()).isEqualTo("PM");
		assertThat(phasesOf(cookie, project.get("id").asLong()).valueStream().toList())
			.hasSize(5)
			.allSatisfy(phase -> assertThat(phase.get("gateApproverUserId").isNull()).isTrue());
	}

	@Test
	@DisplayName("코드는 대문자로 저장되고, 대소문자만 다른 코드는 중복으로 거부된다(409)")
	void duplicateCodeIsRejected() throws Exception {
		final Cookie cookie = loginAs(creator);
		assertThat(readResponse(create(cookie, validCreateBody("erp-1"))).get("code").asString()).isEqualTo("ERP-1");

		expectError(create(cookie, validCreateBody("ERP-1")), ErrorCode.DUPLICATE_PROJECT_CODE);
		expectError(create(cookie, validCreateBody("Erp-1")), ErrorCode.DUPLICATE_PROJECT_CODE);
		assertThat(projectRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("필수 항목(명칭·코드·기간) 누락, 코드 형식 오류는 400이고 아무것도 저장되지 않는다")
	void requiredFieldsAndCodeFormat() throws Exception {
		final Cookie cookie = loginAs(creator);
		for (final String field : List.of("name", "code", "startDate", "endDate")) {
			final Map<String, Object> body = validCreateBody("ERP-1");
			body.put(field, null);
			create(cookie, body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
		}
		for (final String code : List.of("E", "-ERP", "ERP_1", "한글코드", "A".repeat(21))) {
			create(cookie, validCreateBody(code)).andExpect(status().isBadRequest());
		}
		assertThat(projectRepository.count()).isZero();
	}

	@Test
	@DisplayName("시작일이 종료일보다 늦으면 거부하고, 같은 날은 허용한다")
	void periodValidation() throws Exception {
		final Cookie cookie = loginAs(creator);
		final Map<String, Object> reversed = validCreateBody("ERP-1");
		reversed.put("startDate", "2027-04-01");
		expectError(create(cookie, reversed), ErrorCode.INVALID_PROJECT_PERIOD);

		final Map<String, Object> sameDay = validCreateBody("ERP-2");
		sameDay.put("startDate", "2027-03-31");
		create(cookie, sameDay).andExpect(status().isOk());
	}

	@Test
	@DisplayName("MVP에서 비활성인 운영 방식(애자일)·부서 유형(제조)·액터는 서버에서도 거부한다")
	void disabledOptionsAreRejected() throws Exception {
		final Cookie cookie = loginAs(creator);

		final Map<String, Object> agile = validCreateBody("ERP-1");
		agile.put("methodology", "AGILE");
		expectError(create(cookie, agile), ErrorCode.METHODOLOGY_NOT_AVAILABLE);

		final Map<String, Object> manufacturing = validCreateBody("ERP-1");
		manufacturing.put("departmentType", "MANUFACTURING");
		expectError(create(cookie, manufacturing), ErrorCode.DEPARTMENT_TYPE_NOT_AVAILABLE);

		final Map<String, Object> manufacturingActor = validCreateBody("ERP-1");
		manufacturingActor.put("actors", List.of("PM", "DEPT_STAFF"));
		manufacturingActor.remove("members");
		manufacturingActor.put("gateApproverUserId", null);
		expectError(create(cookie, manufacturingActor), ErrorCode.ACTOR_NOT_AVAILABLE);

		assertThat(projectRepository.count()).isZero();
	}

	@Test
	@DisplayName("PM은 해제할 수 없고, 지정하지 않은 액터나 PM 역할로는 멤버를 초대할 수 없다")
	void actorRules() throws Exception {
		final Cookie cookie = loginAs(creator);

		final Map<String, Object> withoutPm = validCreateBody("ERP-1");
		withoutPm.put("actors", List.of("BUSINESS_OWNER", "DEVELOPER"));
		expectError(create(cookie, withoutPm), ErrorCode.PM_ACTOR_REQUIRED);

		final Map<String, Object> unassignedActor = validCreateBody("ERP-1");
		unassignedActor.put("members", List.of(member(developer.getId(), "QA")));
		unassignedActor.put("gateApproverUserId", null);
		expectError(create(cookie, unassignedActor), ErrorCode.MEMBER_ACTOR_NOT_ASSIGNED);

		final Map<String, Object> pmRole = validCreateBody("ERP-1");
		pmRole.put("members", List.of(member(developer.getId(), "PM")));
		pmRole.put("gateApproverUserId", null);
		expectError(create(cookie, pmRole), ErrorCode.PM_ROLE_NOT_INVITABLE);
	}

	@Test
	@DisplayName("초대 대상: 생성자·중복·경영진·비활성·없는 사용자는 거부하고, 다른 부서 사용자는 허용한다")
	void memberRules() throws Exception {
		final Cookie cookie = loginAs(creator);

		expectError(create(cookie, withSingleMember("ERP-1", creator.getId())), ErrorCode.CREATOR_ALREADY_PM);
		expectError(create(cookie, withSingleMember("ERP-1", itExecutive.getId())), ErrorCode.INVALID_PROJECT_MEMBER);
		expectError(create(cookie, withSingleMember("ERP-1", 999_999L)), ErrorCode.INVALID_PROJECT_MEMBER);

		final User leaver = saveUser("leaver01", USER_PASSWORD, SystemRole.USER, devTeam.getId());
		leaver.deactivate();
		userRepository.save(leaver);
		expectError(create(cookie, withSingleMember("ERP-1", leaver.getId())), ErrorCode.INVALID_PROJECT_MEMBER);

		final Map<String, Object> duplicate = validCreateBody("ERP-1");
		duplicate.put("members", List.of(member(developer.getId(), "DEVELOPER"), member(developer.getId(), "BUSINESS_OWNER")));
		duplicate.put("gateApproverUserId", null);
		expectError(create(cookie, duplicate), ErrorCode.DUPLICATE_PROJECT_MEMBER);

		// 현업 담당자는 보통 다른 부서 사람이다(영업팀).
		create(cookie, withSingleMember("ERP-2", businessOwner.getId())).andExpect(status().isOk());
	}

	@Test
	@DisplayName("게이트 승인자는 초대한 멤버 중 PM이 아닌 사람만 지정할 수 있다")
	void gateApproverRules() throws Exception {
		final Cookie cookie = loginAs(creator);

		final Map<String, Object> notInvited = validCreateBody("ERP-1");
		notInvited.put("gateApproverUserId", noPermission.getId());
		expectError(create(cookie, notInvited), ErrorCode.GATE_APPROVER_NOT_MEMBER);

		final Map<String, Object> pmAsApprover = validCreateBody("ERP-1");
		pmAsApprover.put("gateApproverUserId", creator.getId());
		expectError(create(cookie, pmAsApprover), ErrorCode.GATE_APPROVER_CANNOT_BE_PM);

		// 현업 담당자가 아닌 멤버(개발자)도 승인자가 될 수 있다(요구사항 명세서 3.3 'A(지정 시)').
		final Map<String, Object> developerAsApprover = validCreateBody("ERP-2");
		developerAsApprover.put("gateApproverUserId", developer.getId());
		create(cookie, developerAsApprover).andExpect(status().isOk());
	}

	@Test
	@DisplayName("프로젝트 생성 권한이 없는 일반 사용자와 경영진은 403, 시스템 관리자는 프로젝트 API 자체가 404")
	void creationPermission() throws Exception {
		expectError(create(loginAs(noPermission), validCreateBody("ERP-1")), ErrorCode.PROJECT_CREATION_FORBIDDEN);
		expectError(create(loginAs(itExecutive), validCreateBody("ERP-1")), ErrorCode.PROJECT_CREATION_FORBIDDEN);

		saveUser("admin01", USER_PASSWORD, SystemRole.SYSTEM_ADMIN, null);
		final Cookie admin = loginOk("admin01", USER_PASSWORD);
		expectError(create(admin, validCreateBody("ERP-1")), ErrorCode.NOT_FOUND);
		expectError(getAs(admin, PROJECTS), ErrorCode.NOT_FOUND);

		assertThat(projectRepository.count()).isZero();
	}

	@Test
	@DisplayName("주관 부서는 활성 부서여야 한다")
	void departmentMustBeActive() throws Exception {
		final Department closed = saveDepartment("폐지팀", division.getId());
		closed.deactivate();
		departmentRepository.save(closed);
		final Cookie cookie = loginAs(creator);

		final Map<String, Object> inactive = validCreateBody("ERP-1");
		inactive.put("departmentId", closed.getId());
		expectError(create(cookie, inactive), ErrorCode.INVALID_PROJECT_DEPARTMENT);

		final Map<String, Object> missing = validCreateBody("ERP-1");
		missing.put("departmentId", 999_999L);
		expectError(create(cookie, missing), ErrorCode.INVALID_PROJECT_DEPARTMENT);
	}

	@Test
	@DisplayName("코드 중복 확인: 정규화한 코드와 사용 가능 여부를 주고, 생성 권한이 없으면 403")
	void codeAvailability() throws Exception {
		final Cookie cookie = loginAs(creator);
		getAs(cookie, PROJECTS + "/code-availability?code=erp-1")
			.andExpect(jsonPath("$.response.code").value("ERP-1"))
			.andExpect(jsonPath("$.response.available").value(true));

		createProject(validCreateBody("ERP-1"));
		getAs(cookie, PROJECTS + "/code-availability?code=Erp-1")
			.andExpect(jsonPath("$.response.available").value(false));

		getAs(cookie, PROJECTS + "/code-availability?code=-bad").andExpect(status().isBadRequest());
		expectError(getAs(loginAs(noPermission), PROJECTS + "/code-availability?code=ERP-2"), ErrorCode.PROJECT_CREATION_FORBIDDEN);
	}

	@Test
	@DisplayName("생성 선택지: 애자일·제조 액터는 비활성, SW 액터는 활성, PM은 필수로 내려간다")
	void creationOptions() throws Exception {
		getAs(loginAs(developer), PROJECTS + "/creation-options")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.response.methodologies[0].value").value("WATERFALL"))
			.andExpect(jsonPath("$.response.methodologies[0].enabled").value(true))
			.andExpect(jsonPath("$.response.methodologies[1].value").value("AGILE"))
			.andExpect(jsonPath("$.response.methodologies[1].enabled").value(false))
			.andExpect(jsonPath("$.response.departmentTypes[0].value").value("SW"))
			.andExpect(jsonPath("$.response.departmentTypes[0].actors[0].value").value("PM"))
			.andExpect(jsonPath("$.response.departmentTypes[0].actors[0].required").value(true))
			.andExpect(jsonPath("$.response.departmentTypes[0].actors[*].enabled").value(everyItem(is(true))))
			.andExpect(jsonPath("$.response.departmentTypes[1].value").value("MANUFACTURING"))
			.andExpect(jsonPath("$.response.departmentTypes[1].enabled").value(false))
			.andExpect(jsonPath("$.response.departmentTypes[1].actors[*].enabled").value(everyItem(is(false))));
	}

	private Map<String, Object> withSingleMember(final String code, final Long userId) {
		final Map<String, Object> body = validCreateBody(code);
		body.put("members", List.of(member(userId, "DEVELOPER")));
		body.put("gateApproverUserId", null);
		return body;
	}
}
