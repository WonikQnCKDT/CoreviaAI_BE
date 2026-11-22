package org.wonik.coreviaai_be.domain.wbs.policy;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.policy.ProjectAccessPolicy;
import org.wonik.coreviaai_be.domain.project.repository.ProjectFeatureAssignmentRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectRepository;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 계층형 WBS(FR-WBS-01) 접근 판정. 판정 순서는 04. WBS·일정 API 공통 규칙을 따른다.
 * <ol>
 *   <li>볼 수 없는 프로젝트는 404 PROJECT_NOT_FOUND(존재 비노출, ProjectAccessPolicy 재사용)</li>
 *   <li>WBS 기능 모듈을 끈 프로젝트는 404 FEATURE_DISABLED(FR-PRJ-06: 사용하지 않는 모듈은 메뉴에 없음)</li>
 *   <li>변경은 역할에 따라 403(WbsEditor)</li>
 * </ol>
 * 볼 수 있는 사용자(멤버, 공개 범위 열람자, 소속 범위 경영진)는 모두 WBS 전체를 조회할 수 있다(3.2 공통 규칙).
 * 시스템 관리자는 보안 설정에서 /api/projects/**가 이미 차단된다.
 */
@Component
@RequiredArgsConstructor
public class WbsTaskAccessPolicy {

	private final ProjectAccessPolicy accessPolicy;
	private final ProjectRepository projectRepository;
	private final ProjectMemberRepository memberRepository;
	private final ProjectFeatureAssignmentRepository featureRepository;

	public Project getReadableProject(final Long userId, final Long projectId) {
		final Project project = accessPolicy.getVisibleProject(userId, projectId);
		if (!featureRepository.existsByProjectIdAndFeature(projectId, ProjectFeature.WBS)) {
			throw new CustomException(ErrorCode.FEATURE_DISABLED);
		}
		return project;
	}

	/**
	 * 변경 전에 프로젝트 행을 잠근다(PESSIMISTIC_WRITE). 같은 프로젝트의 WBS 변경(생성·수정·이동·삭제, AI 초안 반영)을
	 * 차례로 처리해 표시 순서 중복, 상위 기간 산출 경쟁, 항목 수 상한 초과를 막는다. 판정을 먼저 하고 잠가
	 * 볼 수 없는 사용자는 잠금을 잡지 않는다.
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public WbsEditor getEditor(final Long userId, final Long projectId) {
		getReadableProject(userId, projectId);
		final Project locked = projectRepository.findByIdForUpdate(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
		final ProjectActor actor = memberRepository.findByProjectIdAndUserId(projectId, userId)
			.map(ProjectMember::getActor)
			.orElse(null);
		return new WbsEditor(locked, userId, actor);
	}
}
