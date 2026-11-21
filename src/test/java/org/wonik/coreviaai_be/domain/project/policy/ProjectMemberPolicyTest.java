package org.wonik.coreviaai_be.domain.project.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.service.command.InitialMember;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * FR-PRJ-04 멤버·역할·복수 PM 규칙 중 DB 없이 판정하는 것들(단위 테스트). HTTP·DB 흐름은 ProjectMemberApiIntegrationTest가 다룬다.
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

	@Nested
	@DisplayName("역할 변경")
	class RoleChange {

		@Test
		@DisplayName("일반 역할끼리는 지정 액터 안에서 바꿀 수 있고, 지정하지 않은 액터는 고를 수 없다")
		void ordinary() {
			assertThatCode(() -> policy.validateRoleChange(ASSIGNED, developerMember, ProjectActor.BUSINESS_OWNER, 1, true, false))
				.doesNotThrowAnyException();
			assertError(() -> policy.validateRoleChange(ASSIGNED, developerMember, ProjectActor.QA, 1, true, false),
				ErrorCode.MEMBER_ACTOR_NOT_ASSIGNED);
		}

		@Test
		@DisplayName("PM 추가: 활성 멤버만, 게이트 승인자는 PM이 될 수 없다")
		void addPm() {
			assertThatCode(() -> policy.validateRoleChange(ASSIGNED, developerMember, ProjectActor.PM, 1, true, false))
				.doesNotThrowAnyException();
			assertError(() -> policy.validateRoleChange(ASSIGNED, developerMember, ProjectActor.PM, 1, false, false),
				ErrorCode.INVALID_PM_TARGET);
			assertError(() -> policy.validateRoleChange(ASSIGNED, developerMember, ProjectActor.PM, 1, true, true),
				ErrorCode.PM_TARGET_IS_GATE_APPROVER);
		}

		@Test
		@DisplayName("PM 해제: PM이 2명 이상이면 허용하고, 마지막 1명이면 LAST_PM_REQUIRED")
		void releasePm() {
			assertThatCode(() -> policy.validateRoleChange(ASSIGNED, pmMember, ProjectActor.DEVELOPER, 2, true, false))
				.doesNotThrowAnyException();
			assertError(() -> policy.validateRoleChange(ASSIGNED, pmMember, ProjectActor.DEVELOPER, 1, true, false),
				ErrorCode.LAST_PM_REQUIRED);
			assertError(() -> policy.validateRoleChange(ASSIGNED, pmMember, ProjectActor.DEVELOPER, 0, true, false),
				ErrorCode.LAST_PM_REQUIRED);
		}

		@Test
		@DisplayName("PM 해제 시에는 대상 활성 여부·승인자 여부를 보지 않는다(비활성 PM도 해제 가능)")
		void releaseIgnoresTargetState() {
			assertThatCode(() -> policy.validateRoleChange(ASSIGNED, pmMember, ProjectActor.BUSINESS_OWNER, 2, false, true))
				.doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("제외")
	class Removal {

		@Test
		@DisplayName("일반 멤버는 제외할 수 있고, 게이트 승인자는 제외할 수 없다")
		void ordinary() {
			assertThatCode(() -> policy.validateRemoval(developerMember, false, 1)).doesNotThrowAnyException();
			assertError(() -> policy.validateRemoval(developerMember, true, 1), ErrorCode.GATE_APPROVER_NOT_REMOVABLE);
		}

		@Test
		@DisplayName("PM은 다른 PM이 남을 때만 제외할 수 있다")
		void pm() {
			assertThatCode(() -> policy.validateRemoval(pmMember, false, 2)).doesNotThrowAnyException();
			assertError(() -> policy.validateRemoval(pmMember, false, 1), ErrorCode.LAST_PM_REQUIRED);
		}
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
