package org.wonik.coreviaai_be.domain.wbs.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberRepository;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

/**
 * 직접 등록·수정하는 WBS 항목의 기간·담당자 규칙. 프로젝트 기간 2026-12-01 ~ 2027-03-31(기간 없음은 별도).
 */
class WbsTaskRulesTest {

	private static final long PROJECT_ID = 1L;

	private final ProjectMemberRepository memberRepository = mock(ProjectMemberRepository.class);
	private final UserRepository userRepository = mock(UserRepository.class);
	private final WbsTaskRules rules = new WbsTaskRules(memberRepository, userRepository);

	@Test
	@DisplayName("기간 경계: 프로젝트 첫날·마지막날, 하루짜리 작업은 통과")
	void scheduleBoundaries() {
		final Project project = project(date("2026-12-01"), date("2027-03-31"));

		assertThatCode(() -> {
			rules.validateSchedule(project, date("2026-12-01"), date("2027-03-31"));
			rules.validateSchedule(project, date("2026-12-31"), date("2026-12-31"));
		}).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("기간 위반: 시작일 > 종료일(하루 차이), 프로젝트 시작 전날 시작, 프로젝트 종료 다음날 종료는 INVALID_WBS_SCHEDULE")
	void scheduleViolations() {
		final Project project = project(date("2026-12-01"), date("2027-03-31"));

		expectError(() -> rules.validateSchedule(project, date("2026-12-11"), date("2026-12-10")));
		expectError(() -> rules.validateSchedule(project, date("2026-11-30"), date("2026-12-10")));
		expectError(() -> rules.validateSchedule(project, date("2027-03-01"), date("2027-04-01")));
	}

	@Test
	@DisplayName("프로젝트 기간이 없거나 한쪽만 있으면 있는 쪽만 확인한다")
	void projectWithoutPeriod() {
		assertThatCode(() -> rules.validateSchedule(project(null, null), date("2000-01-01"), date("2099-12-31")))
			.doesNotThrowAnyException();
		assertThatCode(() -> rules.validateSchedule(project(date("2026-12-01"), null), date("2026-12-01"), date("2099-12-31")))
			.doesNotThrowAnyException();
		expectError(() -> rules.validateSchedule(project(null, date("2027-03-31")), date("2027-01-01"), date("2027-04-01")));
	}

	@Test
	@DisplayName("담당자: 미지정 통과, 활성 멤버 통과, 비멤버·비활성 멤버·없는 사용자는 INVALID_TASK_ASSIGNEE")
	void assignee() {
		when(memberRepository.existsByProjectIdAndUserId(PROJECT_ID, 10L)).thenReturn(true);
		when(userRepository.findById(10L)).thenReturn(Optional.of(user(true)));
		when(memberRepository.existsByProjectIdAndUserId(PROJECT_ID, 11L)).thenReturn(true);
		when(userRepository.findById(11L)).thenReturn(Optional.of(user(false)));
		when(memberRepository.existsByProjectIdAndUserId(PROJECT_ID, 12L)).thenReturn(false);
		when(userRepository.findById(12L)).thenReturn(Optional.of(user(true)));
		when(memberRepository.existsByProjectIdAndUserId(PROJECT_ID, 13L)).thenReturn(true);
		when(userRepository.findById(13L)).thenReturn(Optional.empty());

		assertThatCode(() -> {
			rules.validateAssignee(PROJECT_ID, null);
			rules.validateAssignee(PROJECT_ID, 10L);
		}).doesNotThrowAnyException();
		expectAssigneeError(11L);
		expectAssigneeError(12L);
		expectAssigneeError(13L);
	}

	private void expectAssigneeError(final Long userId) {
		assertThatThrownBy(() -> rules.validateAssignee(PROJECT_ID, userId))
			.isInstanceOf(CustomException.class)
			.extracting(error -> ((CustomException)error).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_TASK_ASSIGNEE);
	}

	private static void expectError(final Runnable call) {
		assertThatThrownBy(call::run)
			.isInstanceOf(CustomException.class)
			.extracting(error -> ((CustomException)error).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_WBS_SCHEDULE);
	}

	private static Project project(final LocalDate start, final LocalDate end) {
		final Project project = mock(Project.class);
		when(project.getStartDate()).thenReturn(start);
		when(project.getEndDate()).thenReturn(end);
		return project;
	}

	private static User user(final boolean active) {
		final User user = User.create("user01", "사용자", SystemRole.USER);
		if (!active) {
			user.deactivate();
		}
		return user;
	}

	private static LocalDate date(final String value) {
		return LocalDate.parse(value);
	}
}
