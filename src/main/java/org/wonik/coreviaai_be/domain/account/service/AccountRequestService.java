package org.wonik.coreviaai_be.domain.account.service;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wonik.coreviaai_be.domain.account.entity.AccountRequest;
import org.wonik.coreviaai_be.domain.account.entity.AccountRequestStatus;
import org.wonik.coreviaai_be.domain.account.repository.AccountRequestRepository;
import org.wonik.coreviaai_be.domain.account.service.command.ApproveAccountRequestCommand;
import org.wonik.coreviaai_be.domain.account.service.command.RejectAccountRequestCommand;
import org.wonik.coreviaai_be.domain.account.service.command.SubmitAccountRequestCommand;
import org.wonik.coreviaai_be.domain.account.service.result.AccountRequestResult;
import org.wonik.coreviaai_be.domain.account.service.result.AccountRequestStatusResult;
import org.wonik.coreviaai_be.domain.department.entity.Department;
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
import org.wonik.global.common.type.SystemRole;
import org.wonik.global.user.entity.User;
import org.wonik.global.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * 계정 신청·승인(FR-ADM-10).
 * <p>
 * 신청자는 계정이 없다. 승인은 사용자 행을 활성 상태로 만들고, 비밀번호는 authserver의 관리자 초기화로 따로 발급한다.
 * 그래서 승인 직후에도 비밀번호가 없으면 로그인되지 않는다. 같은 아이디의 대기 신청은 유니크 제약으로 한 건만 남는다.
 */
@Service
@RequiredArgsConstructor
public class AccountRequestService {

	private final AccountRequestRepository accountRequestRepository;
	private final UserRepository userRepository;
	private final DepartmentRepository departmentRepository;
	private final AuditLogger auditLogger;
	private final Clock clock;

	@Transactional
	public AccountRequestResult submit(final String ipAddress, final SubmitAccountRequestCommand command) {
		final String loginId = command.loginId().strip();
		final String name = command.name().strip();
		final String purpose = command.purpose().strip();
		if (command.departmentId() != null) {
			requireAssignableDepartment(command.departmentId());
		}
		if (userRepository.existsByLoginId(loginId)) {
			throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
		}
		if (accountRequestRepository.existsByLoginIdAndStatus(loginId, AccountRequestStatus.PENDING)) {
			throw new CustomException(ErrorCode.DUPLICATE_ACCOUNT_REQUEST);
		}

		final AccountRequest request = AccountRequest.submit(loginId, name, command.departmentId(), purpose);
		final AccountRequest saved = saveNewRequest(request);
		auditLogger.record(
			new AuditActor(null, ipAddress),
			AuditAction.ACCOUNT_REQUEST_SUBMITTED,
			AuditTargetType.ACCOUNT_REQUEST,
			saved.getId(),
			AuditChanges.create()
				.value("loginId", saved.getLoginId())
				.value("name", saved.getName())
				.value("departmentId", saved.getDepartmentId())
				.value("purpose", saved.getPurpose())
		);
		return toResult(saved);
	}

	@Transactional(readOnly = true)
	public AccountRequestStatusResult findOwnStatus(final String loginId, final String name) {
		final AccountRequest request = accountRequestRepository
			.findFirstByLoginIdAndNameOrderByIdDesc(loginId.strip(), name.strip())
			.orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_REQUEST_NOT_FOUND));
		return new AccountRequestStatusResult(
			request.getStatus(),
			request.getRejectReason(),
			request.getCreatedAt(),
			request.getReviewedAt()
		);
	}

	@Transactional(readOnly = true)
	public CursorPage<AccountRequestResult> search(final CursorPageRequest page, final AccountRequestStatus status) {
		final List<AccountRequest> fetched = accountRequestRepository.findPageForAdmin(
			page.cursorId(),
			status,
			page.fetchPageable()
		);
		final CursorPage<AccountRequest> requests = CursorPage.of(fetched, page.size(), AccountRequest::getId);
		final Map<Long, String> departmentNames = departmentNamesOf(requests.items());
		final Map<Long, String> reviewerNames = reviewerNamesOf(requests.items());
		return requests.map(request -> new AccountRequestResult(
			request,
			request.getDepartmentId() == null ? null : departmentNames.get(request.getDepartmentId()),
			request.getReviewedBy() == null ? null : reviewerNames.get(request.getReviewedBy())
		));
	}

	@Transactional
	public AccountRequestResult approve(
		final AuditActor actor,
		final Long requestId,
		final ApproveAccountRequestCommand command
	) {
		final AccountRequest request = getPending(requestId, command.version());
		final Long departmentId = command.departmentId() != null ? command.departmentId() : request.getDepartmentId();
		requireDepartmentForRole(command.systemRole(), departmentId);
		if (departmentId != null) {
			requireAssignableDepartment(departmentId);
		}
		if (userRepository.existsByLoginId(request.getLoginId())) {
			throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
		}

		final User user = User.create(
			request.getLoginId(),
			request.getName(),
			command.systemRole(),
			departmentId,
			normalizeOptional(command.jobTitle())
		);
		final User savedUser = saveNewUser(user);
		request.approve(actor.userId(), savedUser.getId(), departmentId, clock.instant());
		flushReview(request);

		auditLogger.record(actor, AuditAction.USER_CREATED, AuditTargetType.USER, savedUser.getId(), AuditChanges.create()
			.value("loginId", savedUser.getLoginId())
			.value("name", savedUser.getName())
			.value("systemRole", savedUser.getSystemRole())
			.value("departmentId", savedUser.getDepartmentId())
			.value("jobTitle", savedUser.getJobTitle())
			.value("accountRequestId", request.getId()));
		auditLogger.record(actor, AuditAction.USER_ACTIVATED, AuditTargetType.USER, savedUser.getId(), null);
		auditLogger.record(actor, AuditAction.ACCOUNT_REQUEST_APPROVED, AuditTargetType.ACCOUNT_REQUEST, request.getId(),
			AuditChanges.create()
				.value("userId", savedUser.getId())
				.value("systemRole", command.systemRole())
				.value("departmentId", departmentId)
				.value("jobTitle", savedUser.getJobTitle()));
		return toResult(request);
	}

	@Transactional
	public AccountRequestResult reject(
		final AuditActor actor,
		final Long requestId,
		final RejectAccountRequestCommand command
	) {
		final AccountRequest request = getPending(requestId, command.version());
		final String reason = command.rejectReason().strip();
		request.reject(actor.userId(), reason, clock.instant());
		flushReview(request);
		auditLogger.record(actor, AuditAction.ACCOUNT_REQUEST_REJECTED, AuditTargetType.ACCOUNT_REQUEST, request.getId(),
			AuditChanges.create().value("rejectReason", reason));
		return toResult(request);
	}

	private AccountRequest getPending(final Long requestId, final long version) {
		final AccountRequest request = accountRequestRepository.findById(requestId)
			.orElseThrow(() -> new CustomException(ErrorCode.ACCOUNT_REQUEST_NOT_FOUND));
		if (request.getStatus() != AccountRequestStatus.PENDING) {
			throw new CustomException(ErrorCode.ACCOUNT_REQUEST_ALREADY_REVIEWED);
		}
		if (request.getVersion() != version) {
			throw new CustomException(ErrorCode.CONCURRENT_MODIFICATION);
		}
		return request;
	}

	private AccountRequest saveNewRequest(final AccountRequest request) {
		try {
			return accountRequestRepository.saveAndFlush(request);
		} catch (final DataIntegrityViolationException e) {
			throw new CustomException(ErrorCode.DUPLICATE_ACCOUNT_REQUEST, e);
		}
	}

	private User saveNewUser(final User user) {
		try {
			return userRepository.saveAndFlush(user);
		} catch (final DataIntegrityViolationException e) {
			throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID, e);
		}
	}

	private void flushReview(final AccountRequest request) {
		try {
			accountRequestRepository.flush();
		} catch (final ObjectOptimisticLockingFailureException e) {
			throw new CustomException(ErrorCode.CONCURRENT_MODIFICATION, e);
		}
	}

	private void requireAssignableDepartment(final Long departmentId) {
		final Department department = departmentRepository.findById(departmentId)
			.orElseThrow(() -> new CustomException(ErrorCode.DEPARTMENT_NOT_FOUND));
		if (!department.isActive()) {
			throw new CustomException(ErrorCode.DEPARTMENT_INACTIVE);
		}
	}

	private static void requireDepartmentForRole(final SystemRole systemRole, final Long departmentId) {
		if (departmentId == null && systemRole != SystemRole.SYSTEM_ADMIN) {
			throw new CustomException(ErrorCode.DEPARTMENT_REQUIRED);
		}
	}

	private AccountRequestResult toResult(final AccountRequest request) {
		final String departmentName = request.getDepartmentId() == null
			? null
			: departmentRepository.findById(request.getDepartmentId()).map(Department::getName).orElse(null);
		final String reviewerName = request.getReviewedBy() == null
			? null
			: userRepository.findById(request.getReviewedBy()).map(User::getName).orElse(null);
		return new AccountRequestResult(request, departmentName, reviewerName);
	}

	private Map<Long, String> departmentNamesOf(final List<AccountRequest> requests) {
		final Set<Long> departmentIds = requests.stream()
			.map(AccountRequest::getDepartmentId)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		if (departmentIds.isEmpty()) {
			return Map.of();
		}
		return departmentRepository.findAllById(departmentIds).stream()
			.collect(Collectors.toMap(Department::getId, Department::getName));
	}

	private Map<Long, String> reviewerNamesOf(final List<AccountRequest> requests) {
		final Set<Long> reviewerIds = requests.stream()
			.map(AccountRequest::getReviewedBy)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
		if (reviewerIds.isEmpty()) {
			return Map.of();
		}
		return userRepository.findAllById(reviewerIds).stream()
			.collect(Collectors.toMap(User::getId, User::getName));
	}

	private static String normalizeOptional(final String value) {
		if (value == null) {
			return null;
		}
		final String stripped = value.strip();
		return stripped.isEmpty() ? null : stripped;
	}
}
