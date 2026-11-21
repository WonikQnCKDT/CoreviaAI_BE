package org.wonik.coreviaai_be.domain.wbs.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.wonik.coreviaai_be.domain.wbs.service.command.ApplyWbsDraftCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftMilestoneCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftSubtaskCommand;
import org.wonik.coreviaai_be.domain.wbs.service.command.DraftTaskCommand;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * AI 초안 반영 전 서버 검증 규칙. 프로젝트 기간 2026-12-01 ~ 2027-03-31, 멤버 10·11 기준(기간 없음은 별도 테스트).
 */
class WbsDraftValidatorTest {

	private static final LocalDate PROJECT_START = LocalDate.of(2026, 12, 1);
	private static final LocalDate PROJECT_END = LocalDate.of(2027, 3, 31);
	private static final Set<Long> MEMBERS = Set.of(10L, 11L);

	private final WbsDraftValidator validator = new WbsDraftValidator();

	@Test
	@DisplayName("통과: 기간 안의 작업·하위작업, 선후행, 멤버 담당자, 연결된 마일스톤")
	void validDraft() {
		final ApplyWbsDraftCommand command = command(
			List.of(
				task("T-001", "2026-12-01", "2026-12-10", 10L, List.of(),
					List.of(subtask("ST-001", null, null, 11L))),
				task("T-002", "2026-12-11", "2026-12-20", null, List.of("T-001"), List.of())
			),
			List.of(milestone("M-001", "2026-12-20", List.of("T-002")))
		);

		assertThatCode(() -> validate(command)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("tempId가 작업·하위작업·마일스톤 사이에서 겹치면 DUPLICATE_WBS_DRAFT_ITEM")
	void duplicateTempId() {
		final ApplyWbsDraftCommand command = command(
			List.of(task("T-001", "2026-12-01", "2026-12-10", null, List.of(),
				List.of(subtask("T-001", null, null, null)))),
			List.of()
		);

		expectError(command, ErrorCode.DUPLICATE_WBS_DRAFT_ITEM);
	}

	@Test
	@DisplayName("작업 기간이 프로젝트 기간을 벗어나면 INVALID_WBS_DRAFT_SCHEDULE")
	void taskOutsideProject() {
		final ApplyWbsDraftCommand command = command(
			List.of(task("T-001", "2026-11-30", "2026-12-10", null, List.of(), List.of())),
			List.of()
		);

		expectError(command, ErrorCode.INVALID_WBS_DRAFT_SCHEDULE);
	}

	@Test
	@DisplayName("작업 시작일이 종료일보다 늦으면 INVALID_WBS_DRAFT_SCHEDULE")
	void startAfterEnd() {
		final ApplyWbsDraftCommand command = command(
			List.of(task("T-001", "2026-12-10", "2026-12-01", null, List.of(), List.of())),
			List.of()
		);

		expectError(command, ErrorCode.INVALID_WBS_DRAFT_SCHEDULE);
	}

	@Test
	@DisplayName("하위작업 기간이 상위 작업 기간을 벗어나면 INVALID_WBS_DRAFT_SCHEDULE")
	void subtaskOutsideParent() {
		final ApplyWbsDraftCommand command = command(
			List.of(task("T-001", "2026-12-01", "2026-12-10", null, List.of(),
				List.of(subtask("ST-001", "2026-12-05", "2026-12-11", null)))),
			List.of()
		);

		expectError(command, ErrorCode.INVALID_WBS_DRAFT_SCHEDULE);
	}

	@Test
	@DisplayName("마일스톤 목표일이 프로젝트 기간 밖이면 INVALID_WBS_DRAFT_SCHEDULE")
	void milestoneOutsideProject() {
		final ApplyWbsDraftCommand command = command(List.of(), List.of(milestone("M-001", "2027-04-01", List.of())));

		expectError(command, ErrorCode.INVALID_WBS_DRAFT_SCHEDULE);
	}

	@Test
	@DisplayName("반영하지 않는 작업을 선행으로 두면 INVALID_WBS_DRAFT_REFERENCE")
	void dependencyOnMissingTask() {
		final ApplyWbsDraftCommand command = command(
			List.of(task("T-002", "2026-12-11", "2026-12-20", null, List.of("T-001"),
				List.of())),
			List.of()
		);

		expectError(command, ErrorCode.INVALID_WBS_DRAFT_REFERENCE);
	}

	@Test
	@DisplayName("하위작업을 선행으로 두면(작업만 허용) INVALID_WBS_DRAFT_REFERENCE")
	void dependencyOnSubtask() {
		final ApplyWbsDraftCommand command = command(
			List.of(
				task("T-001", "2026-12-01", "2026-12-10", null, List.of(),
					List.of(subtask("ST-001", null, null, null))),
				task("T-002", "2026-12-11", "2026-12-20", null, List.of("ST-001"), List.of())
			),
			List.of()
		);

		expectError(command, ErrorCode.INVALID_WBS_DRAFT_REFERENCE);
	}

	@Test
	@DisplayName("자기 자신을 선행으로 두면 INVALID_WBS_DRAFT_REFERENCE")
	void selfDependency() {
		final ApplyWbsDraftCommand command = command(
			List.of(task("T-001", "2026-12-01", "2026-12-10", null, List.of("T-001"),
				List.of())),
			List.of()
		);

		expectError(command, ErrorCode.INVALID_WBS_DRAFT_REFERENCE);
	}

	@Test
	@DisplayName("선후행이 순환하면 WBS_DEPENDENCY_CYCLE")
	void dependencyCycle() {
		final ApplyWbsDraftCommand command = command(
			List.of(
				task("T-001", "2026-12-01", "2026-12-10", null, List.of("T-003"), List.of()),
				task("T-002", "2026-12-11", "2026-12-20", null, List.of("T-001"), List.of()),
				task("T-003", "2026-12-21", "2026-12-31", null, List.of("T-002"), List.of())
			),
			List.of()
		);

		expectError(command, ErrorCode.WBS_DEPENDENCY_CYCLE);
	}

	@Test
	@DisplayName("마일스톤이 반영하지 않는 작업에 연결되면 INVALID_WBS_DRAFT_REFERENCE")
	void milestoneOnMissingTask() {
		final ApplyWbsDraftCommand command = command(List.of(), List.of(milestone("M-001", "2026-12-20", List.of("T-009"))));

		expectError(command, ErrorCode.INVALID_WBS_DRAFT_REFERENCE);
	}

	@Test
	@DisplayName("멤버가 아닌 담당자(작업·하위작업)면 INVALID_TASK_ASSIGNEE")
	void assigneeNotMember() {
		final ApplyWbsDraftCommand taskAssignee = command(
			List.of(task("T-001", "2026-12-01", "2026-12-10", 99L, List.of(), List.of())),
			List.of()
		);
		final ApplyWbsDraftCommand subtaskAssignee = command(
			List.of(task("T-001", "2026-12-01", "2026-12-10", null, List.of(),
				List.of(subtask("ST-001", null, null, 99L)))),
			List.of()
		);

		expectError(taskAssignee, ErrorCode.INVALID_TASK_ASSIGNEE);
		expectError(subtaskAssignee, ErrorCode.INVALID_TASK_ASSIGNEE);
	}

	@Test
	@DisplayName("프로젝트 기간이 없으면 작업·마일스톤 날짜를 프로젝트 기간으로 제한하지 않는다")
	void projectWithoutPeriod() {
		final ApplyWbsDraftCommand command = command(
			List.of(task("T-001", "2025-01-01", "2030-12-31", null, List.of(), List.of())),
			List.of(milestone("M-001", "2031-01-01", List.of()))
		);

		assertThatCode(() -> validator.validate(command, null, null, MEMBERS)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("프로젝트 시작일만 있으면 시작일보다 이른 작업만 거부한다")
	void projectWithStartOnly() {
		final ApplyWbsDraftCommand early = command(
			List.of(task("T-001", "2026-11-30", "2027-12-31", null, List.of(), List.of())), List.of());
		final ApplyWbsDraftCommand late = command(
			List.of(task("T-001", "2026-12-01", "2030-12-31", null, List.of(), List.of())), List.of());

		assertThatThrownBy(() -> validator.validate(early, PROJECT_START, null, MEMBERS))
			.isInstanceOf(CustomException.class)
			.extracting(error -> ((CustomException)error).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_WBS_DRAFT_SCHEDULE);
		assertThatCode(() -> validator.validate(late, PROJECT_START, null, MEMBERS)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("프로젝트 기간이 없어도 시작일이 종료일보다 늦은 작업은 거부한다")
	void startAfterEndWithoutPeriod() {
		final ApplyWbsDraftCommand command = command(
			List.of(task("T-001", "2026-12-10", "2026-12-01", null, List.of(), List.of())), List.of());

		assertThatThrownBy(() -> validator.validate(command, null, null, MEMBERS))
			.isInstanceOf(CustomException.class)
			.extracting(error -> ((CustomException)error).getErrorCode())
			.isEqualTo(ErrorCode.INVALID_WBS_DRAFT_SCHEDULE);
	}

	private void validate(final ApplyWbsDraftCommand command) {
		validator.validate(command, PROJECT_START, PROJECT_END, MEMBERS);
	}

	private void expectError(final ApplyWbsDraftCommand command, final ErrorCode errorCode) {
		assertThatThrownBy(() -> validate(command))
			.isInstanceOf(CustomException.class)
			.extracting(error -> ((CustomException)error).getErrorCode())
			.isEqualTo(errorCode);
	}

	private static ApplyWbsDraftCommand command(
		final List<DraftTaskCommand> tasks,
		final List<DraftMilestoneCommand> milestones
	) {
		return new ApplyWbsDraftCommand("token", tasks, milestones, 0);
	}

	private static DraftTaskCommand task(
		final String tempId,
		final String start,
		final String end,
		final Long assigneeUserId,
		final List<String> dependencies,
		final List<DraftSubtaskCommand> subtasks
	) {
		return new DraftTaskCommand(tempId, tempId + " 작업", null, LocalDate.parse(start), LocalDate.parse(end),
			BigDecimal.ONE, assigneeUserId, dependencies, subtasks);
	}

	private static DraftSubtaskCommand subtask(
		final String tempId,
		final String start,
		final String end,
		final Long assigneeUserId
	) {
		return new DraftSubtaskCommand(tempId, tempId + " 하위작업", null,
			start == null ? null : LocalDate.parse(start), end == null ? null : LocalDate.parse(end), null, assigneeUserId);
	}

	private static DraftMilestoneCommand milestone(final String tempId, final String targetDate, final List<String> taskIds) {
		return new DraftMilestoneCommand(tempId, tempId + " 마일스톤", LocalDate.parse(targetDate), taskIds);
	}
}
