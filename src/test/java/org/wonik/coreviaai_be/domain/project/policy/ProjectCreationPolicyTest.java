package org.wonik.coreviaai_be.domain.project.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.project.entity.type.DepartmentType;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility;
import org.wonik.coreviaai_be.domain.project.service.command.CreateProjectCommand;
import org.wonik.coreviaai_be.domain.project.service.command.InitialMember;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * FR-PRJ-01 생성 규칙 중 DB 없이 판정하는 것들. HTTP 흐름은 ProjectCreateApiIntegrationTest가 다룬다.
 */
class ProjectCreationPolicyTest {

	private static final long CREATOR = 1L;
	private static final long DEVELOPER = 2L;
	private static final long OWNER = 3L;
	private static final LocalDate START = LocalDate.of(2026, 12, 1);

	private final ProjectCreationPolicy policy = new ProjectCreationPolicy();

	@Test
	@DisplayName("PM만 지정하고 멤버·승인자 없이도 통과한다")
	void minimalCommandPasses() {
		assertThatCode(() -> policy.validate(command(Set.of(ProjectActor.PM), List.of(), null), CREATOR))
			.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("시작일과 종료일이 같으면 통과하고, 시작일이 늦으면 INVALID_PROJECT_PERIOD")
	void period() {
		assertThatCode(() -> policy.validate(withPeriod(START, START), CREATOR)).doesNotThrowAnyException();
		assertError(withPeriod(START.plusDays(1), START), ErrorCode.INVALID_PROJECT_PERIOD);
	}

	@Test
	@DisplayName("SW 프로젝트에서 제조 액터는 고를 수 없고, PM은 빠질 수 없다")
	void actors() {
		assertError(command(Set.of(ProjectActor.PM, ProjectActor.DEPT_STAFF), List.of(), null), ErrorCode.ACTOR_NOT_AVAILABLE);
		assertError(command(Set.of(ProjectActor.DEVELOPER), List.of(), null), ErrorCode.PM_ACTOR_REQUIRED);
	}

	@Test
	@DisplayName("멤버 규칙: 생성자 재초대·PM 역할·미지정 액터·같은 사용자 중복을 거부한다")
	void members() {
		final Set<ProjectActor> actors = Set.of(ProjectActor.PM, ProjectActor.DEVELOPER);
		assertError(command(actors, List.of(new InitialMember(CREATOR, ProjectActor.DEVELOPER)), null), ErrorCode.CREATOR_ALREADY_PM);
		assertError(command(actors, List.of(new InitialMember(DEVELOPER, ProjectActor.PM)), null), ErrorCode.PM_ROLE_NOT_INVITABLE);
		assertError(command(actors, List.of(new InitialMember(DEVELOPER, ProjectActor.QA)), null), ErrorCode.MEMBER_ACTOR_NOT_ASSIGNED);
		assertError(command(actors, List.of(
			new InitialMember(DEVELOPER, ProjectActor.DEVELOPER),
			new InitialMember(DEVELOPER, ProjectActor.DEVELOPER)
		), null), ErrorCode.DUPLICATE_PROJECT_MEMBER);
	}

	@Test
	@DisplayName("게이트 승인자는 생성자(PM)일 수 없고 초대한 멤버여야 한다")
	void gateApprover() {
		final Set<ProjectActor> actors = Set.of(ProjectActor.PM, ProjectActor.BUSINESS_OWNER);
		final List<InitialMember> members = List.of(new InitialMember(OWNER, ProjectActor.BUSINESS_OWNER));
		assertError(command(actors, members, CREATOR), ErrorCode.GATE_APPROVER_CANNOT_BE_PM);
		assertError(command(actors, members, DEVELOPER), ErrorCode.GATE_APPROVER_NOT_MEMBER);
		assertThatCode(() -> policy.validate(command(actors, members, OWNER), CREATOR)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("PM은 부서 유형과 무관한 공통 액터지만, 부서 유형이 비활성이면 함께 선택할 수 없다")
	void actorSelectableFor() {
		assertThat(ProjectActor.PM.selectableFor(DepartmentType.SW)).isTrue();
		assertThat(ProjectActor.QA.selectableFor(DepartmentType.SW)).isTrue();
		assertThat(ProjectActor.PM.selectableFor(DepartmentType.MANUFACTURING)).isFalse();
		assertThat(ProjectActor.DEPT_STAFF.selectableFor(DepartmentType.SW)).isFalse();
	}

	private void assertError(final CreateProjectCommand command, final ErrorCode errorCode) {
		assertThatThrownBy(() -> policy.validate(command, CREATOR))
			.isInstanceOf(CustomException.class)
			.extracting(e -> ((CustomException)e).getErrorCode())
			.isEqualTo(errorCode);
	}

	private static CreateProjectCommand withPeriod(final LocalDate startDate, final LocalDate endDate) {
		return new CreateProjectCommand("이름", "ERP-1", startDate, endDate, 1L, ProjectVisibility.COMPANY,
			Methodology.WATERFALL, DepartmentType.SW, Set.of(ProjectActor.PM), List.of(), null);
	}

	private static CreateProjectCommand command(
		final Set<ProjectActor> actors,
		final List<InitialMember> members,
		final Long gateApproverUserId
	) {
		return new CreateProjectCommand("이름", "ERP-1", START, START.plusMonths(3), 1L, ProjectVisibility.COMPANY,
			Methodology.WATERFALL, DepartmentType.SW, actors, members, gateApproverUserId);
	}
}
