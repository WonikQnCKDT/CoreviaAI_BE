package org.wonik.coreviaai_be.domain.risk.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.ProjectFeatureAssignment;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectTemplate;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility;
import org.wonik.coreviaai_be.domain.project.repository.ProjectFeatureAssignmentRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectRepository;
import org.wonik.coreviaai_be.domain.risk.service.RiskAnalysisRunner;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskDetails;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.coreviaai_be.domain.wbs.repository.WbsTaskRepository;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.support.ApiIntegrationTestSupport;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OverloadQueryApiIntegrationTest extends ApiIntegrationTestSupport {

	private static final LocalDate START = LocalDate.of(2026, 11, 2);
	private static final LocalDate END = LocalDate.of(2026, 11, 13);

	@Autowired
	private RiskAnalysisRunner runner;
	@Autowired
	private ProjectRepository projectRepository;
	@Autowired
	private ProjectFeatureAssignmentRepository featureAssignmentRepository;
	@Autowired
	private ProjectMemberRepository memberRepository;
	@Autowired
	private WbsTaskRepository taskRepository;

	private Long projectId;
	private User pm;
	private User developer;
	private User outsider;
	private Long childDepartmentId;
	private String childDepartmentName;

	@BeforeEach
	void arrange() {
		final var parent = saveDepartment("편중상위", null);
		final var child = saveDepartment("편중하위", parent.getId());
		final var other = saveDepartment("편중외부", null);
		childDepartmentId = child.getId();
		childDepartmentName = child.getName();

		pm = saveUser("ov-pm", USER_PASSWORD, SystemRole.USER, parent.getId());
		developer = saveUser("ov-dev", USER_PASSWORD, SystemRole.USER, child.getId());
		final User qa = saveUser("ov-qa", USER_PASSWORD, SystemRole.USER, child.getId());
		final User business = saveUser("ov-biz", USER_PASSWORD, SystemRole.USER, child.getId());
		outsider = saveUser("ov-out", USER_PASSWORD, SystemRole.USER, other.getId());
		final User edge = saveUser("ov-edge", USER_PASSWORD, SystemRole.USER, child.getId());

		final Project project = projectRepository.save(Project.create(
			"OVLD01", "편중 프로젝트", START, LocalDate.of(2026, 12, 31),
			ProjectVisibility.COMPANY, ProjectTemplate.IT_PROJECT, Methodology.WATERFALL,
			DepartmentType.SW, pm.getId()));
		projectId = project.getId();
		featureAssignmentRepository.save(ProjectFeatureAssignment.of(projectId, ProjectFeature.WBS));
		memberRepository.save(ProjectMember.of(projectId, pm.getId(), ProjectActor.PM));
		memberRepository.save(ProjectMember.of(projectId, developer.getId(), ProjectActor.DEVELOPER));
		memberRepository.save(ProjectMember.of(projectId, qa.getId(), ProjectActor.QA));
		memberRepository.save(ProjectMember.of(projectId, business.getId(), ProjectActor.BUSINESS_OWNER));
		memberRepository.save(ProjectMember.of(projectId, outsider.getId(), ProjectActor.DEVELOPER));
		memberRepository.save(ProjectMember.of(projectId, edge.getId(), ProjectActor.DEVELOPER));

		saveTask(developer, new BigDecimal("12.1"));
		saveTask(outsider, new BigDecimal("12.1"));
		saveTask(edge, new BigDecimal("12.0"));
		saveTask(qa, new BigDecimal("1.0"));

		assertThat(runner.runDaily()).isGreaterThanOrEqualTo(2);
	}

	@Test
	@DisplayName("PM은 임계값을 넘긴 담당자만 보고, 개발자는 본인만 본다")
	void pmSeesOverloadedAssigneesAndDeveloperSeesSelf() throws Exception {
		final Cookie pmCookie = loginOk(pm.getLoginId(), USER_PASSWORD);
		final JsonNode pmView = readResponse(getAs(pmCookie, "/api/projects/{projectId}/risk-overloads", projectId));
		assertThat(pmView.get("scope").asString()).isEqualTo("ASSIGNEES");
		assertThat(pmView.get("thresholdPercent").asInt()).isEqualTo(120);
		assertThat(pmView.get("runDate").asString()).isEqualTo("2026-11-09");
		final List<Long> assigneeIds = new ArrayList<>();
		pmView.get("assignees").forEach(node -> {
			assigneeIds.add(node.get("userId").asLong());
			assertThat(new BigDecimal(node.get("allocationPercent").asString())).isEqualByComparingTo("121.00");
			assertThat(node.get("availableDays").asInt()).isEqualTo(10);
		});
		assertThat(assigneeIds).containsExactlyInAnyOrder(developer.getId(), outsider.getId());
		assertThat(pmView.get("departments").size()).isZero();

		final Cookie devCookie = loginOk(developer.getLoginId(), USER_PASSWORD);
		final JsonNode self = readResponse(getAs(devCookie, "/api/projects/{projectId}/risk-overloads", projectId));
		assertThat(self.get("assignees").size()).isEqualTo(1);
		assertThat(self.get("assignees").get(0).get("userId").asLong()).isEqualTo(developer.getId());
		assertThat(self.get("assignees").get(0).get("name").asString()).doesNotContain(outsider.getName());
	}

	@Test
	@DisplayName("경영진은 소속 범위 부서의 인원 수만 보고 이름과 범위 밖 부서는 없다")
	void executiveSeesDepartmentCountsOnly() throws Exception {
		final User executive = saveUser("ov-exec", USER_PASSWORD, SystemRole.EXECUTIVE, childDepartmentId);
		final Cookie cookie = loginOk(executive.getLoginId(), USER_PASSWORD);
		final JsonNode view = readResponse(getAs(cookie, "/api/projects/{projectId}/risk-overloads", projectId));

		assertThat(view.get("scope").asString()).isEqualTo("DEPARTMENTS");
		assertThat(view.get("assignees").size()).isZero();
		assertThat(view.get("departments")).hasSize(1);
		assertThat(view.get("departments").get(0).get("name").asString()).isEqualTo(childDepartmentName);
		assertThat(view.get("departments").get(0).get("overloadedCount").asInt()).isEqualTo(1);
		assertThat(view.toString()).doesNotContain(developer.getName()).doesNotContain(outsider.getName());
	}

	@Test
	@DisplayName("현업 담당자와 로그인하지 않은 사용자는 조회할 수 없다")
	void businessOwnerAndAnonymousAreRejected() throws Exception {
		final Cookie business = loginOk("ov-biz", USER_PASSWORD);
		expectError(getAs(business, "/api/projects/{projectId}/risk-overloads", projectId),
			ErrorCode.OVERLOAD_VIEW_FORBIDDEN);
		expectError(getAs(null, "/api/projects/{projectId}/risk-overloads", projectId), ErrorCode.UNAUTHORIZED);
	}

	private void saveTask(final User assignee, final BigDecimal effort) {
		taskRepository.save(WbsTask.task(
			projectId,
			taskRepository.findAllByProjectIdOrderBySortOrderAscIdAsc(projectId).size(),
			new TaskDetails("업무", null, assignee.getId(), START, END, effort),
			WbsItemSource.MANUAL,
			pm.getId()));
	}
}
