package org.wonik.coreviaai_be.domain.wbs.policy;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.wbs.entity.TaskDetails;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.coreviaai_be.domain.wbs.entity.type.WbsItemSource;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * 3.3 권한 매트릭스 WBS-01의 역할별 허용·거부. 사용자 10이 판정 대상, 작업 담당자는 10(본인) 또는 20(다른 사람).
 */
class WbsEditorTest {

	private static final long ME = 10L;
	private static final long OTHER = 20L;

	@Test
	@DisplayName("PM: 담당자와 관계없이 하위작업 추가·수정·배정·PM 전용 작업 모두 허용")
	void pmCanDoEverything() {
		final WbsEditor pm = editor(ProjectActor.PM);

		assertThatCode(() -> {
			pm.requirePm();
			pm.requireCanAddSubtaskUnder(taskAssignedTo(OTHER));
			pm.requireCanUpdate(taskAssignedTo(null));
			pm.requireCanAssignOnCreate(OTHER);
		}).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@EnumSource(value = ProjectActor.class, names = {"DEVELOPER", "QA"})
	@DisplayName("개발자·QA: 본인 담당 작업만 수정·하위작업 추가, 담당자는 비우거나 본인만, PM 전용 작업은 403")
	void developerAndQa(final ProjectActor actor) {
		final WbsEditor editor = editor(actor);

		assertThatCode(() -> {
			editor.requireCanAddSubtaskUnder(taskAssignedTo(ME));
			editor.requireCanUpdate(taskAssignedTo(ME));
			editor.requireCanAssignOnCreate(null);
			editor.requireCanAssignOnCreate(ME);
		}).doesNotThrowAnyException();

		expectError(() -> editor.requireCanUpdate(taskAssignedTo(OTHER)), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		expectError(() -> editor.requireCanUpdate(taskAssignedTo(null)), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		expectError(() -> editor.requireCanAddSubtaskUnder(taskAssignedTo(OTHER)), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		expectError(() -> editor.requireCanAssignOnCreate(OTHER), ErrorCode.PROJECT_PM_ONLY);
		expectError(editor::requirePm, ErrorCode.PROJECT_PM_ONLY);
	}

	@Test
	@DisplayName("현업 담당자: 본인이 담당자로 지정돼 있어도 조회만(수정·하위작업 추가 403)")
	void businessOwnerReadOnly() {
		final WbsEditor owner = editor(ProjectActor.BUSINESS_OWNER);

		expectError(() -> owner.requireCanUpdate(taskAssignedTo(ME)), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		expectError(() -> owner.requireCanAddSubtaskUnder(taskAssignedTo(ME)), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		expectError(owner::requirePm, ErrorCode.PROJECT_PM_ONLY);
	}

	@Test
	@DisplayName("멤버가 아닌 열람자·경영진(역할 없음): 모든 변경 403")
	void nonMemberReadOnly() {
		final WbsEditor viewer = editor(null);

		expectError(() -> viewer.requireCanUpdate(taskAssignedTo(ME)), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		expectError(() -> viewer.requireCanAddSubtaskUnder(taskAssignedTo(ME)), ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		expectError(viewer::requirePm, ErrorCode.PROJECT_PM_ONLY);
	}

	private static WbsEditor editor(final ProjectActor actor) {
		return new WbsEditor(null, ME, actor);
	}

	private static WbsTask taskAssignedTo(final Long assigneeUserId) {
		return WbsTask.task(1L, 1, new TaskDetails("작업", null, assigneeUserId, LocalDate.of(2026, 12, 1),
			LocalDate.of(2026, 12, 5), null), WbsItemSource.MANUAL, 1L);
	}

	private static void expectError(final Runnable call, final ErrorCode errorCode) {
		assertThatThrownBy(call::run)
			.isInstanceOf(CustomException.class)
			.extracting(error -> ((CustomException)error).getErrorCode())
			.isEqualTo(errorCode);
	}
}
