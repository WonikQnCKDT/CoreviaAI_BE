package org.wonik.coreviaai_be.domain.project.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectTemplate;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility;
import org.wonik.coreviaai_be.domain.project.service.command.CreateProjectCommand;
import org.wonik.coreviaai_be.domain.project.service.command.InitialMember;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * FR-PRJ-01·06 생성 규칙 중 DB 없이 판정하는 것들(단위 테스트). HTTP·DB 흐름은 ProjectCreateApiIntegrationTest가 다룬다.
 */
class ProjectCreationPolicyTest {

	private static final long CREATOR = 1L;
	private static final long DEVELOPER = 2L;
	private static final long OWNER = 3L;
	private static final LocalDate START = LocalDate.of(2026, 12, 1);

	private final ProjectCreationPolicy policy = new ProjectCreationPolicy();

	@Test
	@DisplayName("PM만 지정하고 기간·주관 부서·멤버·승인자·기능 없이도 통과한다(기간·주관 부서 후입력)")
	void minimalCommandPasses() {
		assertPasses(base().build());
	}

	@Nested
	@DisplayName("기간")
	class Period {

		@Test
		@DisplayName("시작일과 종료일이 같으면 통과하고, 시작일이 하루라도 늦으면 INVALID_PROJECT_PERIOD")
		void order() {
			assertPasses(base().period(START, START).build());
			assertError(base().period(START.plusDays(1), START).build(), ErrorCode.INVALID_PROJECT_PERIOD);
		}

		@Test
		@DisplayName("시작일·종료일 중 하나만 입력해도 통과한다")
		void partial() {
			assertPasses(base().period(START, null).build());
			assertPasses(base().period(null, START).build());
		}
	}

	@Nested
	@DisplayName("주관 부서(0~N개)")
	class Departments {

		@Test
		@DisplayName("0개·1개·여러 개를 허용한다")
		void zeroToMany() {
			assertPasses(base().departments(List.of()).build());
			assertPasses(base().departments(List.of(10L)).build());
			assertPasses(base().departments(List.of(10L, 11L, 12L)).build());
		}

		@Test
		@DisplayName("같은 부서를 두 번 지정하면 DUPLICATE_PROJECT_DEPARTMENT")
		void duplicate() {
			assertError(base().departments(List.of(10L, 11L, 10L)).build(), ErrorCode.DUPLICATE_PROJECT_DEPARTMENT);
		}
	}

	@Nested
	@DisplayName("운영 방식")
	class MethodologyRule {

		@ParameterizedTest
		@EnumSource(value = Methodology.class, names = {"NONE", "WATERFALL"})
		@DisplayName("MVP에서는 단계 없음·폭포수를 고를 수 있다")
		void enabled(final Methodology methodology) {
			assertPasses(base().methodology(methodology).build());
		}

		@Test
		@DisplayName("애자일은 비활성이라 METHODOLOGY_NOT_AVAILABLE")
		void agileDisabled() {
			assertError(base().methodology(Methodology.AGILE).build(), ErrorCode.METHODOLOGY_NOT_AVAILABLE);
		}

		@Test
		@DisplayName("템플릿과 다른 운영 방식도 허용한다(템플릿은 기본값일 뿐)")
		void templateIsOnlyDefault() {
			assertPasses(base().template(ProjectTemplate.EMPTY).methodology(Methodology.WATERFALL).build());
			assertPasses(base().template(ProjectTemplate.IT_PROJECT).methodology(Methodology.NONE).build());
		}
	}

	@Nested
	@DisplayName("액터")
	class Actors {

		@Test
		@DisplayName("SW 프로젝트에서 제조 액터는 고를 수 없고, PM은 빠질 수 없다")
		void actors() {
			assertError(base().actors(Set.of(ProjectActor.PM, ProjectActor.DEPT_STAFF)).build(), ErrorCode.ACTOR_NOT_AVAILABLE);
			assertError(base().actors(Set.of(ProjectActor.DEVELOPER)).build(), ErrorCode.PM_ACTOR_REQUIRED);
		}

		@Test
		@DisplayName("제조 부서 유형은 MVP에서 비활성이라 DEPARTMENT_TYPE_NOT_AVAILABLE")
		void manufacturingDisabled() {
			assertError(base().departmentType(DepartmentType.MANUFACTURING).build(), ErrorCode.DEPARTMENT_TYPE_NOT_AVAILABLE);
		}
	}

	@Nested
	@DisplayName("기능 구성(FR-PRJ-06)")
	class Features {

		@Test
		@DisplayName("IT 프로젝트 템플릿 기본 모듈은 폭포수에서 모두 켤 수 있다")
		void itTemplateDefaults() {
			assertPasses(base().features(ProjectTemplate.IT_PROJECT.getDefaultFeatures()).build());
		}

		@Test
		@DisplayName("산출물·게이트 모듈은 단계 없음 프로젝트에서 켤 수 없다")
		void deliverableNeedsPhases() {
			assertError(base().methodology(Methodology.NONE).features(Set.of(ProjectFeature.DELIVERABLE)).build(),
				ErrorCode.FEATURE_NOT_AVAILABLE);
		}

		@Test
		@DisplayName("Git 연동은 MVP에서 비활성이라 켤 수 없다")
		void gitDisabled() {
			assertError(base().features(Set.of(ProjectFeature.GIT)).build(), ErrorCode.FEATURE_NOT_AVAILABLE);
		}
	}

	@Test
	@DisplayName("멤버 규칙: 생성자 재초대·PM 역할·미지정 액터·같은 사용자 중복을 거부한다")
	void members() {
		final Set<ProjectActor> actors = Set.of(ProjectActor.PM, ProjectActor.DEVELOPER);
		assertError(base().actors(actors).members(List.of(new InitialMember(CREATOR, ProjectActor.DEVELOPER))).build(),
			ErrorCode.CREATOR_ALREADY_PM);
		assertError(base().actors(actors).members(List.of(new InitialMember(DEVELOPER, ProjectActor.PM))).build(),
			ErrorCode.PM_ROLE_NOT_INVITABLE);
		assertError(base().actors(actors).members(List.of(new InitialMember(DEVELOPER, ProjectActor.QA))).build(),
			ErrorCode.MEMBER_ACTOR_NOT_ASSIGNED);
		assertError(base().actors(actors).members(List.of(
			new InitialMember(DEVELOPER, ProjectActor.DEVELOPER),
			new InitialMember(DEVELOPER, ProjectActor.DEVELOPER)
		)).build(), ErrorCode.DUPLICATE_PROJECT_MEMBER);
	}

	@Nested
	@DisplayName("게이트 승인자")
	class GateApprover {

		private final Set<ProjectActor> actors = Set.of(ProjectActor.PM, ProjectActor.BUSINESS_OWNER);
		private final List<InitialMember> members = List.of(new InitialMember(OWNER, ProjectActor.BUSINESS_OWNER));

		@Test
		@DisplayName("폭포수에서는 초대한 PM 아닌 멤버만 지정할 수 있다")
		void waterfall() {
			assertPasses(base().actors(actors).members(members).gateApprover(OWNER).build());
			assertError(base().actors(actors).members(members).gateApprover(CREATOR).build(), ErrorCode.GATE_APPROVER_CANNOT_BE_PM);
			assertError(base().actors(actors).members(members).gateApprover(DEVELOPER).build(), ErrorCode.GATE_APPROVER_NOT_MEMBER);
		}

		@Test
		@DisplayName("단계 없음 프로젝트에서는 지정할 수 없다")
		void noPhases() {
			assertError(base().methodology(Methodology.NONE).actors(actors).members(members).gateApprover(OWNER).build(),
				ErrorCode.GATE_APPROVER_NOT_AVAILABLE);
		}
	}

	private void assertPasses(final CreateProjectCommand command) {
		assertThatCode(() -> policy.validate(command, CREATOR)).doesNotThrowAnyException();
	}

	private void assertError(final CreateProjectCommand command, final ErrorCode errorCode) {
		assertThatThrownBy(() -> policy.validate(command, CREATOR))
			.isInstanceOf(CustomException.class)
			.extracting(e -> ((CustomException)e).getErrorCode())
			.isEqualTo(errorCode);
	}

	private static CommandBuilder base() {
		return new CommandBuilder();
	}

	/** 테스트마다 바꾸는 항목만 드러나도록 기본값(폭포수·SW·PM만)을 둔 빌더. */
	private static final class CommandBuilder {
		private LocalDate startDate;
		private LocalDate endDate;
		private List<Long> departmentIds = List.of();
		private ProjectTemplate template = ProjectTemplate.IT_PROJECT;
		private Methodology methodology = Methodology.WATERFALL;
		private DepartmentType departmentType = DepartmentType.SW;
		private Set<ProjectActor> actors = Set.of(ProjectActor.PM);
		private Set<ProjectFeature> features = Set.of();
		private List<InitialMember> members = List.of();
		private Long gateApproverUserId;

		CommandBuilder period(final LocalDate start, final LocalDate end) {
			this.startDate = start;
			this.endDate = end;
			return this;
		}

		CommandBuilder departments(final List<Long> ids) {
			this.departmentIds = ids;
			return this;
		}

		CommandBuilder template(final ProjectTemplate value) {
			this.template = value;
			return this;
		}

		CommandBuilder methodology(final Methodology value) {
			this.methodology = value;
			return this;
		}

		CommandBuilder departmentType(final DepartmentType value) {
			this.departmentType = value;
			return this;
		}

		CommandBuilder actors(final Set<ProjectActor> value) {
			this.actors = value;
			return this;
		}

		CommandBuilder features(final Set<ProjectFeature> value) {
			this.features = value;
			return this;
		}

		CommandBuilder members(final List<InitialMember> value) {
			this.members = value;
			return this;
		}

		CommandBuilder gateApprover(final Long value) {
			this.gateApproverUserId = value;
			return this;
		}

		CreateProjectCommand build() {
			return new CreateProjectCommand("이름", "ERP-1", startDate, endDate, departmentIds, ProjectVisibility.COMPANY,
				template, methodology, departmentType, actors, features, members, gateApproverUserId);
		}
	}
}
