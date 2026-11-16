package org.wonik.coreviaai_be.domain.project.policy;

import org.springframework.stereotype.Component;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.repository.ProjectRepository;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 자원 단위 접근 판정. 조회 API와 변경 API가 같은 기준을 쓰도록 한 곳에 둔다.
 * <ul>
 *   <li>볼 수 없는 프로젝트는 없는 프로젝트와 구분하지 않고 404(존재 비노출, 요구사항 명세서 3.2 공개 범위)</li>
 *   <li>볼 수는 있지만 PM이 아니면 403(3.3 권한 매트릭스: 멤버·역할 변경은 PM만, 경영진은 읽기 전용)</li>
 * </ul>
 * 시스템 관리자는 보안 설정에서 /api/projects/**가 이미 차단되므로 여기서 다루지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ProjectAccessPolicy {

	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;
	private final ProjectVisibilityPolicy visibilityPolicy;

	/** 사용자는 삭제하지 않고 비활성화하므로(FR-ADM-02) 유효한 세션의 사용자는 항상 있다. 없으면 비정상 상태라 USER_NOT_FOUND로 끝낸다. */
	public User getViewer(final Long viewerId) {
		return userRepository.findById(viewerId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	public Project getVisibleProject(final Long viewerId, final Long projectId) {
		final User viewer = getViewer(viewerId);
		final Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
		if (!canView(viewer, project)) {
			throw new CustomException(ErrorCode.PROJECT_NOT_FOUND);
		}
		return project;
	}

	/** 현재 PM만 다룰 수 있는 프로젝트(멤버 초대·역할 변경·제외·PM 위임). */
	public Project getManagedProject(final Long userId, final Long projectId) {
		final Project project = getVisibleProject(userId, projectId);
		if (!project.getPmUserId().equals(userId)) {
			throw new CustomException(ErrorCode.PROJECT_PM_ONLY);
		}
		return project;
	}

	private boolean canView(final User viewer, final Project project) {
		if (viewer.getSystemRole() == SystemRole.EXECUTIVE) {
			return visibilityPolicy.canExecutiveView(visibilityPolicy.executiveScope(viewer.getDepartmentId()), project);
		}
		return visibilityPolicy.canUserView(viewer.getId(), viewer.getDepartmentId(), project);
	}
}
