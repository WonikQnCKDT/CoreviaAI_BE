package org.wonik.coreviaai_be.domain.project.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.type.PreviousPmAction;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.service.command.DelegatePmCommand;
import org.wonik.coreviaai_be.domain.project.service.command.InitialMember;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * FR-PRJ-04 멤버·역할·위임 규칙 중 DB 없이 판정하는 것들. HTTP 흐름은 ProjectMemberApiIntegrationTest 등이 다룬다.
 * 프로젝트 지정 액터는 PM·현업 담당자·개발자(QA 미지정)로 둔다.
 */
class ProjectMemberPolicyTest {

	private static final long PROJECT = 10L;
	private static final long PM = 1L;
	private static final long DEVELOPER = 2L;
	private static final long OWNER = 3L;
	private static final Set<ProjectActor> ASSIGNED =
		Set.of(ProjectActor.PM, ProjectActor.BUSINESS_OWNER, ProjectActor.DEVELOPER);

	private final ProjectMemberPolicy policy = new ProjectMemberPolicy();

	private final ProjectMember pmMember = ProjectMember.of(PROJECT, PM, ProjectActor.PM);
	private final ProjectMember developerMember = ProjectMember.of(PROJECT, DEVELOPER, ProjectActor.DEVELOPER);

	@Test
	@DisplayName("초대: 지정 액터 중 PM이 아닌 역할만, 요청 안 중복과 기존 멤버는 거부한다")
	void invite() {
		assertThatCode(() -> policy.validateInvite(ASSIGNED, List.of(member(OWNER, ProjectActor.BUSINESS_OWNER)), List.of()))
			.doesNotThrowAnyException();

		assertError(() -> policy.validateInvite(ASSIGNED, List.of(member(OWNER, ProjectActor.PM)), List.of()),
			ErrorCode.PM_ROLE_NOT_INVITABLE);
		assertError(() -> policy.validateInvite(ASSIGNED, List.of(member(OWNER, ProjectActor.QA)), List.of()),
			ErrorCode.MEMBER_ACTOR_NOT_ASSIGNED);
		assertError(() -> policy.validateInvite(ASSIGNED,
				List.of(member(OWNER, ProjectActor.DEVELOPER), member(OWNER, ProjectActor.BUSINESS_OWNER)), List.of()),
			ErrorCode.DUPLICATE_PROJECT_MEMBER);
		assertError(() -> policy.validateInvite(ASSIGNED, List.of(member(DEVELOPER, ProjectActor.DEVELOPER)), List.of(DEVELOPER)),
			ErrorCode.ALREADY_PROJECT_MEMBER);
	}

	@Test
	@DisplayName("역할 변경: PM의 역할을 바꾸거나 PM으로 바꿀 수 없고, 지정하지 않은 액터는 고를 수 없다")
	void roleChange() {
		assertThatCode(() -> policy.validateRoleChange(ASSIGNED, developerMember, ProjectActor.BUSINESS_OWNER))
			.doesNotThrowAnyException();

		assertError(() -> policy.validateRoleChange(ASSIGNED, pmMember, ProjectActor.DEVELOPER), ErrorCode.PM_ROLE_NOT_CHANGEABLE);
		assertError(() -> policy.validateRoleChange(ASSIGNED, developerMember, ProjectActor.PM), ErrorCode.PM_ROLE_NOT_CHANGEABLE);
		assertError(() -> policy.validateRoleChange(ASSIGNED, developerMember, ProjectActor.QA), ErrorCode.MEMBER_ACTOR_NOT_ASSIGNED);
	}

	@Test
	@DisplayName("제외: PM과 게이트 승인자는 제외할 수 없다")
	void removal() {
		assertThatCode(() -> policy.validateRemoval(developerMember, false)).doesNotThrowAnyException();

		assertError(() -> policy.validateRemoval(pmMember, false), ErrorCode.CANNOT_REMOVE_PM);
		assertError(() -> policy.validateRemoval(developerMember, true), ErrorCode.GATE_APPROVER_NOT_REMOVABLE);
	}

	@Test
	@DisplayName("위임 대상: 본인이 아닌 활성 멤버여야 하고, 게이트 승인자는 PM이 될 수 없다")
	void delegationTarget() {
		final DelegatePmCommand leave = delegate(DEVELOPER, PreviousPmAction.LEAVE, null);
		assertThatCode(() -> policy.validateDelegation(ASSIGNED, PM, leave, developerMember, true, false))
			.doesNotThrowAnyException();

		assertError(() -> policy.validateDelegation(ASSIGNED, PM, leave, null, true, false),
			ErrorCode.INVALID_PM_DELEGATION_TARGET);
		assertError(() -> policy.validateDelegation(ASSIGNED, PM, delegate(PM, PreviousPmAction.LEAVE, null), pmMember, true, false),
			ErrorCode.INVALID_PM_DELEGATION_TARGET);
		assertError(() -> policy.validateDelegation(ASSIGNED, PM, leave, developerMember, false, false),
			ErrorCode.INVALID_PM_DELEGATION_TARGET);
		assertError(() -> policy.validateDelegation(ASSIGNED, PM, leave, developerMember, true, true),
			ErrorCode.PM_TARGET_IS_GATE_APPROVER);
	}

	@Test
	@DisplayName("기존 PM 처리: 역할 변경은 PM이 아닌 지정 액터가 필요하고, 멤버 제외는 역할을 받지 않는다")
	void previousPmAction() {
		assertThatCode(() -> validateDelegation(PreviousPmAction.CHANGE_ROLE, ProjectActor.BUSINESS_OWNER))
			.doesNotThrowAnyException();

		assertError(() -> validateDelegation(PreviousPmAction.CHANGE_ROLE, null), ErrorCode.INVALID_PREVIOUS_PM_ACTION);
		assertError(() -> validateDelegation(PreviousPmAction.CHANGE_ROLE, ProjectActor.PM), ErrorCode.INVALID_PREVIOUS_PM_ACTION);
		assertError(() -> validateDelegation(PreviousPmAction.CHANGE_ROLE, ProjectActor.QA), ErrorCode.MEMBER_ACTOR_NOT_ASSIGNED);
		assertError(() -> validateDelegation(PreviousPmAction.LEAVE, ProjectActor.DEVELOPER), ErrorCode.INVALID_PREVIOUS_PM_ACTION);
	}

	private void validateDelegation(final PreviousPmAction action, final ProjectActor previousPmActor) {
		policy.validateDelegation(ASSIGNED, PM, delegate(DEVELOPER, action, previousPmActor), developerMember, true, false);
	}

	private static DelegatePmCommand delegate(final long target, final PreviousPmAction action, final ProjectActor actor) {
		return new DelegatePmCommand(target, action, actor);
	}

	private static InitialMember member(final long userId, final ProjectActor actor) {
		return new InitialMember(userId, actor);
	}

	private static void assertError(final ThrowingCallable call, final ErrorCode errorCode) {
		assertThatThrownBy(call)
			.isInstanceOf(CustomException.class)
			.satisfies(e -> assertThat(((CustomException)e).getErrorCode()).isEqualTo(errorCode));
	}
}
