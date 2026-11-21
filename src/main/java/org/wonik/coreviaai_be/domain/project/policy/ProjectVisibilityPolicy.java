package org.wonik.coreviaai_be.domain.project.policy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.coreviaai_be.domain.department.repository.DepartmentRepository;
import org.wonik.coreviaai_be.domain.project.entity.Project;
import org.wonik.coreviaai_be.domain.project.entity.type.ProjectVisibility;
import org.wonik.coreviaai_be.domain.project.repository.ProjectDepartmentRepository;
import org.wonik.coreviaai_be.domain.project.repository.ProjectMemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * 프로젝트 공개 범위 판정(요구사항 명세서 3.1·3.2). 시스템 관리자는 보안 설정에서 이미 차단되므로 여기서 다루지 않는다.
 * 경영진의 '소속 범위'는 본인 부서와 그 모든 하위 부서로 해석한다. 주관 부서는 0~N개이며(FR-PRJ-01),
 * 부서 공개·소속 범위 판정은 주관 부서 중 하나라도 맞으면 통과한다. 주관 부서가 없는 부서 공개 프로젝트는 멤버만 볼 수 있다.
 */
@Component
@RequiredArgsConstructor
public class ProjectVisibilityPolicy {

	private final ProjectMemberRepository projectMemberRepository;
	private final ProjectDepartmentRepository projectDepartmentRepository;
	private final DepartmentRepository departmentRepository;

	/** 일반 사용자가 프로젝트를 조회할 수 있는지. 멤버이거나, 전사 공개이거나, 내 부서가 주관 부서인 부서 공개 프로젝트여야 한다. */
	public boolean canUserView(final Long viewerId, final Long viewerDepartmentId, final Project project) {
		if (project.getVisibility() == ProjectVisibility.COMPANY) {
			return true;
		}
		if (project.getVisibility() == ProjectVisibility.DEPARTMENT && viewerDepartmentId != null
			&& projectDepartmentRepository.existsByProjectIdAndDepartmentIdIn(project.getId(), Set.of(viewerDepartmentId))) {
			return true;
		}
		return projectMemberRepository.existsByProjectIdAndUserId(project.getId(), viewerId);
	}

	/** 경영진이 프로젝트를 조회할 수 있는지. 주관 부서가 소속 범위에 있으면 멤버 전용을 포함해 모두, 그 밖은 전사 공개만. */
	public boolean canExecutiveView(final Set<Long> scopeDepartmentIds, final Project project) {
		if (project.getVisibility() == ProjectVisibility.COMPANY) {
			return true;
		}
		return !scopeDepartmentIds.isEmpty()
			&& projectDepartmentRepository.existsByProjectIdAndDepartmentIdIn(project.getId(), scopeDepartmentIds);
	}

	/**
	 * 경영진의 소속 범위: 본인 부서와 모든 하위 부서의 ID. 부서 수가 적어 전체를 읽어 메모리에서 내려간다.
	 * 부서가 없는 경영진은 빈 집합을 받아 전사 공개만 보게 된다.
	 */
	public Set<Long> executiveScope(final Long executiveDepartmentId) {
		if (executiveDepartmentId == null) {
			return Set.of();
		}
		final Map<Long, List<Long>> childrenByParent = new HashMap<>();
		for (final Department department : departmentRepository.findAll()) {
			if (department.getParentId() != null) {
				childrenByParent.computeIfAbsent(department.getParentId(), key -> new ArrayList<>())
					.add(department.getId());
			}
		}
		final Set<Long> scope = new HashSet<>();
		final Deque<Long> queue = new ArrayDeque<>();
		queue.add(executiveDepartmentId);
		while (!queue.isEmpty()) {
			final Long current = queue.poll();
			if (scope.add(current)) {
				queue.addAll(childrenByParent.getOrDefault(current, List.of()));
			}
		}
		return scope;
	}
}
