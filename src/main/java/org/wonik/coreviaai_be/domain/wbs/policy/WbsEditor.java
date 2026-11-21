package org.wonik.coreviaai_be.domain.wbs.policy;

import java.util.EnumSet;
import java.util.Set;

import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.wbs.entity.WbsTask;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

/**
 * WBS를 바꾸려는 사용자와 그 사용자의 프로젝트 역할(3.3 권한 매트릭스 WBS-01).
 * <ul>
 *   <li>PM: 작업·하위작업 생성·수정·삭제·이동, 담당자 배정 모두</li>
 *   <li>개발자·QA: 본인이 담당한 작업 아래 하위작업 생성, 본인이 담당한 작업·하위작업 수정. 담당자 배정·이동·삭제는 불가</li>
 *   <li>그 밖(현업 담당자, 멤버가 아닌 공개 범위 열람자, 경영진): 조회만</li>
 * </ul>
 * 역할은 enum으로 판정한다(역할 이름 문자열 비교 금지, 보안 지침).
 *
 * @param project 잠근 프로젝트 행
 * @param actor   프로젝트 역할. 멤버가 아니면 null
 */
public record WbsEditor(Project project, Long userId, ProjectActor actor) {

	/** 본인 담당 작업을 고치고 그 아래 하위작업을 추가할 수 있는 역할. */
	private static final Set<ProjectActor> OWN_TASK_EDITORS = EnumSet.of(ProjectActor.DEVELOPER, ProjectActor.QA);

	public boolean isPm() {
		return actor == ProjectActor.PM;
	}

	public void requirePm() {
		if (!isPm()) {
			throw new CustomException(ErrorCode.PROJECT_PM_ONLY);
		}
	}

	/** 하위작업 추가: PM이거나, 상위 작업을 담당한 개발자·QA. */
	public void requireCanAddSubtaskUnder(final WbsTask parent) {
		requirePmOrAssignee(parent);
	}

	/** 기본 필드 수정: PM이거나, 그 작업을 담당한 개발자·QA. */
	public void requireCanUpdate(final WbsTask task) {
		requirePmOrAssignee(task);
	}

	/**
	 * 담당자 지정은 업무 배정이라 PM만 한다(3.2 PM 권한 '업무 배정'). 개발자·QA가 하위작업을 만들 때는
	 * 담당자를 비우거나 본인으로만 지정할 수 있다.
	 */
	public void requireCanAssignOnCreate(final Long assigneeUserId) {
		if (!isPm() && assigneeUserId != null && !assigneeUserId.equals(userId)) {
			throw new CustomException(ErrorCode.PROJECT_PM_ONLY);
		}
	}

	private void requirePmOrAssignee(final WbsTask task) {
		if (isPm()) {
			return;
		}
		if (actor == null || !OWN_TASK_EDITORS.contains(actor) || !task.isAssignedTo(userId)) {
			throw new CustomException(ErrorCode.WBS_TASK_ASSIGNEE_ONLY);
		}
	}
}
