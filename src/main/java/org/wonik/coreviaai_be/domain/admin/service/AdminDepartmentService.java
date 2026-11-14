package org.wonik.coreviaai_be.domain.admin.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.admin.service.result.AdminDepartmentResult;
import org.wonik.coreviaai_be.domain.department.entity.Department;
import org.wonik.coreviaai_be.domain.department.entity.DepartmentStatus;
import org.wonik.coreviaai_be.domain.department.repository.DepartmentRepository;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditChanges;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.user.entity.UserStatus;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 시스템 관리자의 부서(조직) 관리(FR-ADM-02, UC-ADM-02).
 * <p>
 * 부서는 사용자 소속과 이후 부서 공개 범위의 기준이므로 계층이 깨지지 않게 막는다: 상위 부서는 활성이어야 하고,
 * 자기 자신·하위 부서 밑으로 옮길 수 없으며, 활성 인원이나 활성 하위 부서가 있으면 비활성화할 수 없다.
 * 변경은 감사 로그와 같은 트랜잭션으로 남겨, 변경과 이력이 어긋나지 않게 한다.
 */
@Service
@RequiredArgsConstructor
public class AdminDepartmentService {

	/** 잘못된 데이터로 계층에 순환이 이미 생겼을 때 무한 반복을 막는 상한. 실제 조직 깊이보다 충분히 크다. */
	private static final int MAX_HIERARCHY_DEPTH = 100;

	private final DepartmentRepository departmentRepository;
	private final UserRepository userRepository;
	private final AuditLogger auditLogger;

	@Transactional(readOnly = true)
	public CursorPage<AdminDepartmentResult> findPage(final CursorPageRequest page, final DepartmentStatus status) {
		final List<Department> fetched = departmentRepository.findPageForAdmin(
			page.cursorId(),
			status,
			page.fetchPageable()
		);
		final CursorPage<Department> departments = CursorPage.of(fetched, page.size(), Department::getId);
		final Map<Long, Long> memberCounts = activeMemberCounts(departments.items());
		return departments.map(department ->
			new AdminDepartmentResult(department, memberCounts.getOrDefault(department.getId(), 0L))
		);
	}

	@Transactional
	public AdminDepartmentResult create(final AuditActor actor, final String rawName, final Long parentId) {
		final String name = rawName.strip();
		if (parentId != null) {
			requireActiveParent(parentId);
		}
		requireNameAvailable(parentId, name);

		final Department department = saveAndFlush(Department.create(parentId, name));
		auditLogger.record(actor, AuditAction.DEPARTMENT_CREATED, AuditTargetType.DEPARTMENT, department.getId(),
			AuditChanges.create().value("name", name).value("parentId", parentId));
		return new AdminDepartmentResult(department, 0L);
	}

	/** 같은 값이면 아무것도 바꾸지 않아, 재전송된 요청이 이력을 늘리지 않는다. */
	@Transactional
	public AdminDepartmentResult update(
		final AuditActor actor,
		final Long departmentId,
		final String rawName,
		final Long parentId
	) {
		final Department department = getDepartment(departmentId);
		final String name = rawName.strip();
		final boolean parentChanged = !Objects.equals(department.getParentId(), parentId);
		final boolean nameChanged = !department.getName().equals(name);
		if (!parentChanged && !nameChanged) {
			return toResult(department);
		}

		if (parentChanged && parentId != null) {
			requireActiveParent(parentId);
			requireNoCycle(department.getId(), parentId);
		}
		requireNameAvailable(parentId, name);

		final AuditChanges changes = AuditChanges.create()
			.change("name", department.getName(), name)
			.change("parentId", department.getParentId(), parentId);
		department.rename(name);
		department.moveTo(parentId);
		saveAndFlush(department);
		auditLogger.record(actor, AuditAction.DEPARTMENT_CHANGED, AuditTargetType.DEPARTMENT, department.getId(), changes);
		return toResult(department);
	}

	@Transactional
	public AdminDepartmentResult changeStatus(
		final AuditActor actor,
		final Long departmentId,
		final DepartmentStatus status
	) {
		final Department department = getDepartment(departmentId);
		if (department.getStatus() == status) {
			return toResult(department);
		}

		final AuditAction action;
		if (status == DepartmentStatus.INACTIVE) {
			if (departmentRepository.existsByParentIdAndStatus(departmentId, DepartmentStatus.ACTIVE)
				|| userRepository.existsByDepartmentIdAndStatus(departmentId, UserStatus.ACTIVE)) {
				throw new CustomException(ErrorCode.DEPARTMENT_IN_USE);
			}
			department.deactivate();
			action = AuditAction.DEPARTMENT_DEACTIVATED;
		} else {
			if (department.getParentId() != null) {
				requireActiveParent(department.getParentId());
			}
			department.activate();
			action = AuditAction.DEPARTMENT_ACTIVATED;
		}
		auditLogger.record(actor, action, AuditTargetType.DEPARTMENT, departmentId, null);
		return toResult(department);
	}

	private Department getDepartment(final Long departmentId) {
		return departmentRepository.findById(departmentId)
			.orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
	}

	private void requireActiveParent(final Long parentId) {
		final boolean active = departmentRepository.findById(parentId)
			.map(Department::isActive)
			.orElse(false);
		if (!active) {
			throw new CustomException(ErrorCode.INVALID_DEPARTMENT_PARENT);
		}
	}

	/** 새 상위 부서에서 최상위까지 올라가다 자기 자신을 만나면 자기 또는 자기 하위 밑으로 옮기는 것이므로 거부한다. */
	private void requireNoCycle(final Long departmentId, final Long newParentId) {
		final Set<Long> visited = new HashSet<>();
		Long current = newParentId;
		while (current != null) {
			if (current.equals(departmentId) || !visited.add(current) || visited.size() > MAX_HIERARCHY_DEPTH) {
				throw new CustomException(ErrorCode.INVALID_DEPARTMENT_PARENT);
			}
			current = departmentRepository.findById(current)
				.map(Department::getParentId)
				.orElse(null);
		}
	}

	private void requireNameAvailable(final Long parentId, final String name) {
		final boolean exists = parentId == null
			? departmentRepository.existsByParentIdIsNullAndName(name)
			: departmentRepository.existsByParentIdAndName(parentId, name);
		if (exists) {
			throw new CustomException(ErrorCode.DUPLICATE_DEPARTMENT_NAME);
		}
	}

	/** 사전 검사를 통과한 뒤 동시에 같은 상위·이름으로 저장된 경우, DB 유니크 제약 위반을 중복 오류로 바꾼다. */
	private Department saveAndFlush(final Department department) {
		try {
			return departmentRepository.saveAndFlush(department);
		} catch (final DataIntegrityViolationException e) {
			throw new CustomException(ErrorCode.DUPLICATE_DEPARTMENT_NAME, e);
		}
	}

	private AdminDepartmentResult toResult(final Department department) {
		return new AdminDepartmentResult(
			department,
			userRepository.countByDepartmentIdAndStatus(department.getId(), UserStatus.ACTIVE)
		);
	}

	private Map<Long, Long> activeMemberCounts(final List<Department> departments) {
		if (departments.isEmpty()) {
			return Map.of();
		}
		final Set<Long> ids = departments.stream().map(Department::getId).collect(Collectors.toSet());
		return userRepository.countByDepartmentIds(ids, UserStatus.ACTIVE).stream()
			.collect(Collectors.toMap(row -> (Long)row[0], row -> (Long)row[1]));
	}
}
