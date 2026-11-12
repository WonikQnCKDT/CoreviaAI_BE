package org.wonik.coreviaai_be.domain.admin.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.wonik.coreviaai_be.domain.admin.service.command.CreateUserCommand;
import org.wonik.coreviaai_be.domain.admin.service.command.UpdateUserProfileCommand;
import org.wonik.coreviaai_be.domain.admin.service.result.AdminUserResult;
import org.wonik.coreviaai_be.domain.admin.service.result.UserHistoryResult;
import org.wonik.global.audit.AuditAction;
import org.wonik.global.audit.AuditActor;
import org.wonik.global.audit.AuditChanges;
import org.wonik.global.audit.AuditLog;
import org.wonik.global.audit.AuditLogRepository;
import org.wonik.global.audit.AuditLogger;
import org.wonik.global.audit.AuditTargetType;
import org.wonik.global.common.exception.CustomException;
import org.wonik.global.common.exception.ErrorCode;
import org.wonik.global.common.pagination.CursorPage;
import org.wonik.global.common.pagination.CursorPageRequest;
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.department.entity.Department;
import org.wonik.global.department.repository.DepartmentRepository;
import org.wonik.global.security.UserSessionTerminator;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.entity.UserStatus;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 시스템 관리자의 사용자 계정 관리(FR-ADM-02, UC-ADM-02).
 * <p>
 * 로그인 비밀번호는 다루지 않는다. 계정을 만든 뒤 authserver의 관리자 비밀번호 초기화로 임시 비밀번호를 발급한다
 * (사용자 정보와 로그인 정보의 소유 모듈을 나눈 구조를 유지하기 위해).
 * <p>
 * 세션은 시스템 역할을 저장해 두고 인가에 쓰므로, 역할 변경·비활성화는 DB 커밋 뒤 대상 사용자의 세션을 끊어야
 * 바뀐 권한이 즉시 적용된다. 세션 저장소 호출이 DB 트랜잭션을 붙잡지 않도록 이 두 메서드만 TransactionTemplate을 쓴다.
 * 세션 끊기가 실패하면 503을 돌려주고, 관리자가 같은 요청을 다시 보내면 DB는 그대로 두고 세션만 다시 끊는다.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

	private final UserRepository userRepository;
	private final DepartmentRepository departmentRepository;
	private final AuditLogRepository auditLogRepository;
	private final AuditLogger auditLogger;
	private final UserSessionTerminator sessionTerminator;
	private final TransactionTemplate transactionTemplate;

	// ---- 조회 ----

	@Transactional(readOnly = true)
	public CursorPage<AdminUserResult> search(
		final CursorPageRequest page,
		final Long departmentId,
		final UserStatus status,
		final SystemRole systemRole,
		final String keyword
	) {
		final List<User> fetched = userRepository.searchForAdmin(
			page.cursorId(),
			departmentId,
			status,
			systemRole,
			toKeywordPattern(keyword),
			page.fetchPageable()
		);
		final CursorPage<User> users = CursorPage.of(fetched, page.size(), User::getId);
		final Map<Long, String> departmentNames = departmentNamesOf(users.items());
		return users.map(user -> new AdminUserResult(user, departmentNames.get(user.getDepartmentId())));
	}

	@Transactional(readOnly = true)
	public AdminUserResult get(final Long userId) {
		return toResult(getUser(userId));
	}

	@Transactional(readOnly = true)
	public CursorPage<UserHistoryResult> findHistories(final Long userId, final CursorPageRequest page) {
		if (!userRepository.existsById(userId)) {
			throw new CustomException(ErrorCode.USER_NOT_FOUND);
		}
		final List<AuditLog> fetched = auditLogRepository.findByTarget(
			AuditTargetType.USER,
			userId,
			page.cursorId(),
			page.fetchPageable()
		);
		final CursorPage<AuditLog> logs = CursorPage.of(fetched, page.size(), AuditLog::getId);
		final Set<Long> actorIds = logs.items().stream()
			.map(AuditLog::getActorUserId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		final Map<Long, String> actorNames = userRepository.findAllById(actorIds).stream()
			.collect(Collectors.toMap(User::getId, User::getName));
		return logs.map(auditLog -> new UserHistoryResult(auditLog, actorNames.get(auditLog.getActorUserId())));
	}

	// ---- 변경 ----

	/** loginId가 자연 키다. 같은 요청이 재전송되면 두 번째는 DUPLICATE_LOGIN_ID(409)로 끝나고 계정은 하나만 생긴다. */
	@Transactional
	public AdminUserResult create(final AuditActor actor, final CreateUserCommand command) {
		final String name = command.name().strip();
		final String jobTitle = normalizeOptional(command.jobTitle());

		if (userRepository.existsByLoginId(command.loginId())) {
			throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
		}
		requireDepartmentForRole(command.systemRole(), command.departmentId());
		if (command.departmentId() != null) {
			requireAssignableDepartment(command.departmentId());
		}
		if (command.canCreateProject() && command.systemRole() != SystemRole.USER) {
			throw new CustomException(ErrorCode.PROJECT_CREATION_NOT_ALLOWED);
		}

		final User user = User.create(command.loginId(), name, command.systemRole(), command.departmentId(), jobTitle);
		if (command.canCreateProject()) {
			user.grantProjectCreation();
		}
		final User saved = saveAndFlushNewUser(user);
		auditLogger.record(actor, AuditAction.USER_CREATED, AuditTargetType.USER, saved.getId(), AuditChanges.create()
			.value("loginId", saved.getLoginId())
			.value("name", saved.getName())
			.value("systemRole", saved.getSystemRole())
			.value("departmentId", saved.getDepartmentId())
			.value("jobTitle", saved.getJobTitle())
			.value("canCreateProject", saved.isCanCreateProject()));
		return toResult(saved);
	}

	/** 같은 값이면 아무것도 바꾸지 않아, 재전송된 요청이 이력을 늘리지 않는다. */
	@Transactional
	public AdminUserResult updateProfile(
		final AuditActor actor,
		final Long userId,
		final UpdateUserProfileCommand command
	) {
		final User user = getUser(userId);
		final String name = command.name().strip();
		final String jobTitle = normalizeOptional(command.jobTitle());

		requireDepartmentForRole(user.getSystemRole(), command.departmentId());
		// 이미 소속된 부서가 나중에 비활성화돼도 이름·직무 수정은 막지 않도록, 부서를 바꿀 때만 활성 여부를 본다.
		if (!Objects.equals(user.getDepartmentId(), command.departmentId()) && command.departmentId() != null) {
			requireAssignableDepartment(command.departmentId());
		}

		final AuditChanges changes = AuditChanges.create()
			.change("name", user.getName(), name)
			.change("departmentId", user.getDepartmentId(), command.departmentId())
			.change("jobTitle", user.getJobTitle(), jobTitle);
		if (changes.isEmpty()) {
			return toResult(user);
		}
		user.changeProfile(name, command.departmentId(), jobTitle);
		auditLogger.record(actor, AuditAction.USER_PROFILE_CHANGED, AuditTargetType.USER, userId, changes);
		return toResult(user);
	}

	public AdminUserResult changeSystemRole(final AuditActor actor, final Long userId, final SystemRole systemRole) {
		final AdminUserResult result = transactionTemplate.execute(transaction -> {
			final User user = getUser(userId);
			requireNotSelf(actor, userId);
			if (user.getSystemRole() == systemRole) {
				return toResult(user);
			}
			if (user.getSystemRole() == SystemRole.SYSTEM_ADMIN && user.isActive()) {
				requireAnotherActiveAdmin(userId);
			}
			requireDepartmentForRole(systemRole, user.getDepartmentId());

			final SystemRole beforeRole = user.getSystemRole();
			final boolean beforeProjectCreation = user.isCanCreateProject();
			user.changeSystemRole(systemRole);
			auditLogger.record(actor, AuditAction.USER_ROLE_CHANGED, AuditTargetType.USER, userId, AuditChanges.create()
				.change("systemRole", beforeRole, systemRole)
				.change("canCreateProject", beforeProjectCreation, user.isCanCreateProject()));
			return toResult(user);
		});
		terminateSessions(userId);
		return result;
	}

	public AdminUserResult changeStatus(final AuditActor actor, final Long userId, final UserStatus status) {
		final AdminUserResult result = transactionTemplate.execute(transaction -> {
			final User user = getUser(userId);
			requireNotSelf(actor, userId);
			if (user.getStatus() == status) {
				return toResult(user);
			}

			if (status == UserStatus.INACTIVE) {
				if (user.getSystemRole() == SystemRole.SYSTEM_ADMIN) {
					requireAnotherActiveAdmin(userId);
				}
				user.deactivate();
				auditLogger.record(actor, AuditAction.USER_DEACTIVATED, AuditTargetType.USER, userId, null);
			} else {
				requireDepartmentForRole(user.getSystemRole(), user.getDepartmentId());
				if (user.getDepartmentId() != null) {
					requireAssignableDepartment(user.getDepartmentId());
				}
				user.activate();
				auditLogger.record(actor, AuditAction.USER_ACTIVATED, AuditTargetType.USER, userId, null);
			}
			return toResult(user);
		});
		if (status == UserStatus.INACTIVE) {
			terminateSessions(userId);
		}
		return result;
	}

	/** 같은 값이면 아무것도 바꾸지 않는다. 프로젝트 생성 권한은 요구사항 명세서 3.1에 따라 일반 사용자만 가진다. */
	@Transactional
	public AdminUserResult changeProjectCreation(final AuditActor actor, final Long userId, final boolean enabled) {
		final User user = getUser(userId);
		if (user.isCanCreateProject() == enabled) {
			return toResult(user);
		}
		if (enabled) {
			if (user.getSystemRole() != SystemRole.USER) {
				throw new CustomException(ErrorCode.PROJECT_CREATION_NOT_ALLOWED);
			}
			user.grantProjectCreation();
			auditLogger.record(actor, AuditAction.PROJECT_CREATION_GRANTED, AuditTargetType.USER, userId, null);
		} else {
			user.revokeProjectCreation();
			auditLogger.record(actor, AuditAction.PROJECT_CREATION_REVOKED, AuditTargetType.USER, userId, null);
		}
		return toResult(user);
	}

	/** 세션 저장소 장애는 DB 변경이 이미 커밋된 뒤라 되돌리지 않고 503으로 알려, 같은 요청 재전송으로 세션만 다시 끊게 한다. */
	private void terminateSessions(final Long userId) {
		try {
			sessionTerminator.terminateAllSessions(userId);
		} catch (final DataAccessException e) {
			throw new CustomException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE, e);
		}
	}

	// ---- 검증 ----

	private User getUser(final Long userId) {
		return userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
	}

	/** 관리자가 실수로 자기 권한을 잃어 아무도 복구할 수 없게 되는 상황을 막는다. */
	private static void requireNotSelf(final AuditActor actor, final Long userId) {
		if (userId.equals(actor.userId())) {
			throw new CustomException(ErrorCode.CANNOT_CHANGE_OWN_ACCOUNT);
		}
	}

	/** 부서 공개 범위·경영진 소속 범위가 부서를 기준으로 하므로 시스템 관리자 외에는 부서가 있어야 한다. */
	private static void requireDepartmentForRole(final SystemRole systemRole, final Long departmentId) {
		if (departmentId == null && systemRole != SystemRole.SYSTEM_ADMIN) {
			throw new CustomException(ErrorCode.DEPARTMENT_REQUIRED);
		}
	}

	private void requireAssignableDepartment(final Long departmentId) {
		final Department department = departmentRepository.findById(departmentId)
			.orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
		if (!department.isActive()) {
			throw new CustomException(ErrorCode.DEPARTMENT_INACTIVE);
		}
	}

	/** 활성 관리자 행을 잠근 뒤 확인해, 두 관리자가 서로를 동시에 강등·비활성화해 0명이 되는 경쟁을 막는다. */
	private void requireAnotherActiveAdmin(final Long targetUserId) {
		final boolean anotherAdminRemains = userRepository
			.findAllBySystemRoleAndStatusForUpdate(SystemRole.SYSTEM_ADMIN, UserStatus.ACTIVE).stream()
			.anyMatch(admin -> !admin.getId().equals(targetUserId));
		if (!anotherAdminRemains) {
			throw new CustomException(ErrorCode.LAST_SYSTEM_ADMIN);
		}
	}

	/** 사전 검사를 통과한 뒤 같은 아이디가 동시에 저장된 경우, DB 유니크 제약 위반을 중복 오류로 바꾼다. */
	private User saveAndFlushNewUser(final User user) {
		try {
			return userRepository.saveAndFlush(user);
		} catch (final DataIntegrityViolationException e) {
			throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID, e);
		}
	}

	// ---- 변환 ----

	private AdminUserResult toResult(final User user) {
		final String departmentName = user.getDepartmentId() == null
			? null
			: departmentRepository.findById(user.getDepartmentId()).map(Department::getName).orElse(null);
		return new AdminUserResult(user, departmentName);
	}

	private Map<Long, String> departmentNamesOf(final List<User> users) {
		final Set<Long> departmentIds = users.stream()
			.map(User::getDepartmentId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		return departmentRepository.findAllById(departmentIds).stream()
			.collect(Collectors.toMap(Department::getId, Department::getName));
	}

	/** 선택 입력의 공백만 있는 값은 '없음'으로 저장한다. */
	private static String normalizeOptional(final String value) {
		if (value == null) {
			return null;
		}
		final String stripped = value.strip();
		return stripped.isEmpty() ? null : stripped;
	}

	/** 사용자가 입력한 %, _, 역슬래시가 LIKE 와일드카드로 해석되지 않고 문자 그대로 검색되도록 이스케이프한다. */
	private static String toKeywordPattern(final String keyword) {
		final String normalized = normalizeOptional(keyword);
		if (normalized == null) {
			return null;
		}
		final String escaped = normalized.toLowerCase(Locale.ROOT)
			.replace("\\", "\\\\")
			.replace("%", "\\%")
			.replace("_", "\\_");
		return "%" + escaped + "%";
	}
}
