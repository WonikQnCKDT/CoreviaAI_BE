package org.wonik.coreviaai_be.domain.project.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.coreviaai_be.domain.department.repository.DepartmentRepository;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.ProjectActorAssignment;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.ProjectPhase;
import org.wonik.coreviaai_be.domain.project.entity.type.Methodology;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectStatus;
import org.wonik.coreviaai_be.domain.project.policy.ProjectVisibilityPolicy;
import org.wonik.coreviaai_be.domain.project.repository.ProjectActorAssignmentRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectPhaseRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectRepository;
import org.wonik.coreviaai_be.domain.project.service.result.ProjectDetailResult;
import org.wonik.coreviaai_be.domain.project.service.result.ProjectMemberResult;
import org.wonik.coreviaai_be.domain.project.service.result.ProjectPhasesResult;
import org.wonik.coreviaai_be.domain.project.service.result.ProjectSummaryResult;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 목록·상세 조회(FR-PRJ-01). 공개 범위(요구사항 명세서 3.1·3.2) 밖의 프로젝트는 목록에 나오지 않고,
 * 상세는 존재 자체를 드러내지 않도록 PROJECT_NOT_FOUND로 응답한다. 시스템 관리자는 보안 설정에서 이미 차단된다.
 */
@Service
@RequiredArgsConstructor
public class ProjectQueryService {

	private final ProjectRepository projectRepository;
	private final ProjectActorAssignmentRepository actorAssignmentRepository;
	private final ProjectMemberRepository memberRepository;
	private final ProjectPhaseRepository phaseRepository;
	private final UserRepository userRepository;
	private final DepartmentRepository departmentRepository;
	private final ProjectVisibilityPolicy visibilityPolicy;

	/** 최신 생성순(id 내림차순). null인 필터는 적용하지 않는다. */
	@Transactional(readOnly = true)
	public CursorPage<ProjectSummaryResult> findPage(
		final Long viewerId,
		final CursorPageRequest page,
		final ProjectStatus status,
		final Long departmentId,
		final Long pmUserId
	) {
		final User viewer = getViewer(viewerId);
		final List<Project> fetched = viewer.getSystemRole() == SystemRole.EXECUTIVE
			? findPageForExecutive(viewer, page, status, departmentId, pmUserId)
			: projectRepository.findPageForUser(
				page.cursorId(), status, departmentId, pmUserId,
				viewer.getId(), viewer.getDepartmentId(), page.fetchPageable());
		final CursorPage<Project> projects = CursorPage.of(fetched, page.size(), Project::getId);
		final Map<Long, String> departmentNames = departmentNamesOf(
			projects.items().stream().map(Project::getDepartmentId).collect(Collectors.toSet()));
		final Map<Long, String> userNames = userNamesOf(
			projects.items().stream().map(Project::getPmUserId).collect(Collectors.toSet()));
		return projects.map(project -> new ProjectSummaryResult(
			project,
			departmentNames.get(project.getDepartmentId()),
			userNames.get(project.getPmUserId())
		));
	}

	@Transactional(readOnly = true)
	public ProjectDetailResult getDetail(final Long viewerId, final Long projectId) {
		final Project project = getVisibleProject(viewerId, projectId);

		final List<ProjectActor> actors = actorAssignmentRepository.findAllByProjectIdOrderById(projectId).stream()
			.map(ProjectActorAssignment::getActor)
			.sorted()
			.toList();
		return new ProjectDetailResult(
			project,
			departmentNamesOf(Set.of(project.getDepartmentId())).get(project.getDepartmentId()),
			actors,
			userNamesOf(Set.of(project.getPmUserId()))
		);
	}

	/** 멤버 목록(커서, id 오름차순 = 등록순). 공개 범위는 상세와 같다. */
	@Transactional(readOnly = true)
	public CursorPage<ProjectMemberResult> findMembers(
		final Long viewerId,
		final Long projectId,
		final CursorPageRequest page
	) {
		getVisibleProject(viewerId, projectId);
		final CursorPage<ProjectMember> members = CursorPage.of(
			memberRepository.findPageByProjectId(projectId, page.cursorId(), page.fetchPageable()),
			page.size(),
			ProjectMember::getId
		);
		final Map<Long, String> userNames = userNamesOf(
			members.items().stream().map(ProjectMember::getUserId).collect(Collectors.toSet()));
		return members.map(member -> new ProjectMemberResult(member, userNames.get(member.getUserId())));
	}

	/**
	 * 폭포수 단계·게이트. 단계가 없는 운영 방식(애자일 등)이면 PHASES_NOT_AVAILABLE로 알려,
	 * FE가 빈 목록을 "단계가 아직 없음"으로 오해하지 않게 한다.
	 */
	@Transactional(readOnly = true)
	public ProjectPhasesResult getPhases(final Long viewerId, final Long projectId) {
		final Project project = getVisibleProject(viewerId, projectId);
		if (project.getMethodology() != Methodology.WATERFALL) {
			throw new CustomException(ErrorCode.PHASES_NOT_AVAILABLE);
		}
		final List<ProjectPhase> phases = phaseRepository.findAllByProjectIdOrderByPhaseOrder(projectId);
		final Set<Long> approverIds = phases.stream()
			.map(ProjectPhase::getGateApproverUserId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		return new ProjectPhasesResult(phases, userNamesOf(approverIds));
	}

	/** 없는 프로젝트와 볼 수 없는 프로젝트를 구분하지 않는다(존재 비노출). */
	private Project getVisibleProject(final Long viewerId, final Long projectId) {
		final User viewer = getViewer(viewerId);
		final Project project = projectRepository.findById(projectId)
			.orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));
		if (!canView(viewer, project)) {
			throw new CustomException(ErrorCode.PROJECT_NOT_FOUND);
		}
		return project;
	}

	/**
	 * 경영진은 소속 범위(본인 부서와 모든 하위 부서)의 프로젝트를 멤버 전용까지 모두 보고, 그 밖은 전사 공개만 본다.
	 * 부서가 없는 경영진은 소속 범위가 비어 전사 공개만 보이므로, IN 절이 빈 쿼리 대신 일반 사용자 쿼리를 부서 없이 쓴다
	 * (경영진은 멤버로 초대될 수 없어 멤버 조건에 걸리는 프로젝트도 없다).
	 */
	private List<Project> findPageForExecutive(
		final User viewer,
		final CursorPageRequest page,
		final ProjectStatus status,
		final Long departmentId,
		final Long pmUserId
	) {
		final Set<Long> scope = visibilityPolicy.executiveScope(viewer.getDepartmentId());
		if (scope.isEmpty()) {
			return projectRepository.findPageForUser(
				page.cursorId(), status, departmentId, pmUserId, viewer.getId(), null, page.fetchPageable());
		}
		return projectRepository.findPageForExecutive(
			page.cursorId(), status, departmentId, pmUserId, scope, page.fetchPageable());
	}

	private boolean canView(final User viewer, final Project project) {
		if (viewer.getSystemRole() == SystemRole.EXECUTIVE) {
			return visibilityPolicy.canExecutiveView(visibilityPolicy.executiveScope(viewer.getDepartmentId()), project);
		}
		return visibilityPolicy.canUserView(viewer.getId(), viewer.getDepartmentId(), project);
	}

	/** 사용자는 삭제하지 않고 비활성화하므로(FR-ADM-02) 유효한 세션의 사용자는 항상 있다. 없으면 비정상 상태라 USER_NOT_FOUND로 끝낸다. */
	private User getViewer(final Long viewerId) {
		return userRepository.findById(viewerId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	private Map<Long, String> departmentNamesOf(final Collection<Long> departmentIds) {
		if (departmentIds.isEmpty()) {
			return Map.of();
		}
		return departmentRepository.findAllById(departmentIds).stream()
			.collect(Collectors.toMap(Department::getId, Department::getName));
	}

	private Map<Long, String> userNamesOf(final Collection<Long> userIds) {
		if (userIds.isEmpty()) {
			return Map.of();
		}
		return userRepository.findAllById(userIds).stream()
			.collect(Collectors.toMap(User::getId, User::getName));
	}
}
