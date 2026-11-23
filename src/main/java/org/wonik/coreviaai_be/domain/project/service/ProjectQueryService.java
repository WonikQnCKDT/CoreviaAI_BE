package org.wonik.coreviaai_be.domain.project.service;

import java.util.Collection;
import java.util.Comparator;
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
import org.wonik.coreviaai_be.domain.project.entity.ProjectDepartment;
import org.wonik.coreviaai_be.domain.project.entity.ProjectFeatureAssignment;
import org.wonik.coreviaai_be.domain.project.entity.ProjectMember;
import org.wonik.coreviaai_be.domain.project.entity.ProjectPhase;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectActor;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectFeature;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectStatus;
import org.wonik.coreviaai_be.domain.project.policy.ProjectAccessPolicy;
import org.wonik.coreviaai_be.domain.project.policy.ProjectVisibilityPolicy;
import org.wonik.coreviaai_be.domain.project.repository.ProjectActorAssignmentRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectDepartmentRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectFeatureAssignmentRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectPhaseRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectRepository;
import org.wonik.coreviaai_be.domain.project.service.result.NamedRef;
import org.wonik.coreviaai_be.domain.project.service.result.ProjectDetailResult;
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
	private final ProjectDepartmentRepository projectDepartmentRepository;
	private final ProjectFeatureAssignmentRepository featureAssignmentRepository;
	private final ProjectMemberRepository memberRepository;
	private final ProjectPhaseRepository phaseRepository;
	private final UserRepository userRepository;
	private final DepartmentRepository departmentRepository;
	private final ProjectVisibilityPolicy visibilityPolicy;
	private final ProjectAccessPolicy accessPolicy;

	/** 최신 생성순(id 내림차순). null인 필터는 적용하지 않는다. */
	@Transactional(readOnly = true)
	public CursorPage<ProjectSummaryResult> findPage(
		final Long viewerId,
		final CursorPageRequest page,
		final ProjectStatus status,
		final Long departmentId,
		final Long pmUserId
	) {
		final User viewer = accessPolicy.getViewer(viewerId);
		final List<Project> fetched = viewer.getSystemRole() == SystemRole.EXECUTIVE
			? findPageForExecutive(viewer, page, status, departmentId, pmUserId)
			: projectRepository.findPageForUser(
				page.cursorId(), status, departmentId, pmUserId,
				viewer.getId(), viewer.getDepartmentId(), page.fetchPageable());
		final CursorPage<Project> projects = CursorPage.of(fetched, page.size(), Project::getId);
		final Set<Long> projectIds = projects.items().stream().map(Project::getId).collect(Collectors.toSet());
		if (projectIds.isEmpty()) {
			return projects.map(project -> new ProjectSummaryResult(project, List.of(), List.of()));
		}
		// 한 페이지의 주관 부서·PM을 각각 한 번에 읽어 N+1 query를 피한다.
		final Map<Long, List<Long>> departmentIdsByProject = projectDepartmentRepository.findAllByProjectIdIn(projectIds)
			.stream()
			.sorted(Comparator.comparing(ProjectDepartment::getDepartmentId))
			.collect(Collectors.groupingBy(ProjectDepartment::getProjectId,
				Collectors.mapping(ProjectDepartment::getDepartmentId, Collectors.toList())));
		final Map<Long, List<Long>> pmUserIdsByProject = memberRepository.findAllByProjectIdInAndActor(projectIds, ProjectActor.PM)
			.stream()
			.sorted(Comparator.comparing(ProjectMember::getId))
			.collect(Collectors.groupingBy(ProjectMember::getProjectId,
				Collectors.mapping(ProjectMember::getUserId, Collectors.toList())));
		final Map<Long, String> departmentNames = departmentNamesOf(flatten(departmentIdsByProject));
		final Map<Long, String> userNames = userNamesOf(flatten(pmUserIdsByProject));
		return projects.map(project -> new ProjectSummaryResult(
			project,
			namedRefs(departmentIdsByProject.getOrDefault(project.getId(), List.of()), departmentNames),
			namedRefs(pmUserIdsByProject.getOrDefault(project.getId(), List.of()), userNames)
		));
	}

	@Transactional(readOnly = true)
	public ProjectDetailResult getDetail(final Long viewerId, final Long projectId) {
		final Project project = accessPolicy.getVisibleProject(viewerId, projectId);

		final List<ProjectActor> actors = actorAssignmentRepository.findAllByProjectIdOrderById(projectId).stream()
			.map(ProjectActorAssignment::getActor)
			.sorted()
			.toList();
		final List<ProjectFeature> features = featureAssignmentRepository.findAllByProjectIdOrderById(projectId).stream()
			.map(ProjectFeatureAssignment::getFeature)
			.sorted()
			.toList();
		final List<Long> departmentIds = projectDepartmentRepository.findAllByProjectIdOrderById(projectId).stream()
			.map(ProjectDepartment::getDepartmentId)
			.sorted()
			.toList();
		final List<Long> pmUserIds = memberRepository.findAllByProjectIdAndActorOrderById(projectId, ProjectActor.PM)
			.stream()
			.map(ProjectMember::getUserId)
			.toList();
		return new ProjectDetailResult(
			project,
			namedRefs(departmentIds, departmentNamesOf(departmentIds)),
			namedRefs(pmUserIds, userNamesOf(pmUserIds)),
			actors,
			features
		);
	}

	/**
	 * 프로젝트의 사용자 정의 단계·게이트. 단계가 없으면 정상적인 빈 목록을 반환한다.
	 */
	@Transactional(readOnly = true)
	public ProjectPhasesResult getPhases(final Long viewerId, final Long projectId) {
		accessPolicy.getVisibleProject(viewerId, projectId);
		final List<ProjectPhase> phases = phaseRepository.findAllByProjectIdOrderByPhaseOrder(projectId);
		final Set<Long> approverIds = phases.stream()
			.map(ProjectPhase::getGateApproverUserId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		return new ProjectPhasesResult(phases, userNamesOf(approverIds));
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

	private static Set<Long> flatten(final Map<Long, List<Long>> idsByProject) {
		return idsByProject.values().stream().flatMap(List::stream).collect(Collectors.toSet());
	}

	private static List<NamedRef> namedRefs(final List<Long> ids, final Map<Long, String> names) {
		return ids.stream().map(id -> new NamedRef(id, names.get(id))).toList();
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
